package ru.rsce.cansat.granum.spectrometer.client.mavlink;

import ru.rsce.cansat.granum.spectrometer.client.netty.SpectrometerClient;
import jssc.SerialPort;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.DataTransmissionHandshake;
import io.dronefleet.mavlink.common.EncapsulatedData;
import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import ru.rsce.cansat.granum.spectrometer.client.FrameMessageProcessor;
import ru.rsce.cansat.granum.spectrometer.client.netty.FrameMessage;
import ru.rsce.cansat.granum.spectrometer.client.netty.Message;

public class SpectrometerClientMavlink extends SpectrometerClient {
    
    public SpectrometerClientMavlink(String serPortName) {
        this.serPort = new SerialPort(serPortName);
        thread = new MavlinkThread(this);
    }
    
    @Override
    public void start() throws Exception {
        serPort.openPort();
        serPort.setParams(921600, jssc.SerialPort.DATABITS_8, jssc.SerialPort.STOPBITS_1, jssc.SerialPort.PARITY_NONE);
        
        connection = MavlinkConnection.create(new SerialInputStream(serPort), new DummyOutputStream());
        thread.start();
    }
    
    @Override
    public void waitExit() {}
    
    private class MavlinkThread extends Thread {
        private final int headerSize = 4 * 4;
        
        public MavlinkThread(SpectrometerClientMavlink client){
            this.client = client;
        }
    
        @Override
        public void run(){
            MavlinkMessage msg;

            while(true) {
                try { //TODO proper exception handling
                    msg = client.connection.next();
                    
                    if(msg.getPayload().getClass() == DataTransmissionHandshake.class) {
                        //System.out.println("Handshake!");
                        try {
                            pushFrame();
                        } catch(NullPointerException ex) {}

                        DataTransmissionHandshake handshake = (DataTransmissionHandshake) msg.getPayload();
                        buffer = buffer((int)handshake.size() + headerSize);

                        buffer.writeIntLE(handshake.width());
                        buffer.writeIntLE(handshake.height());
                        buffer.writeIntLE((int)FrameMessage.PixFmt.fourccFromString("  40"));
                        buffer.writeIntLE((int)handshake.size());

                        totalFrames = handshake.packets();
                        currFrame = 0;
                    }
                    
                    if(msg.getPayload().getClass() == EncapsulatedData.class) {
                        //System.out.print("Data #");
                        EncapsulatedData data = (EncapsulatedData) msg.getPayload();
                        //System.out.println(data.seqnr());
                        try {
                            if(currFrame > data.seqnr()) { //We've lost packets alltogether with handshake
                                buffer = null;
                                continue;
                            }
                            
                            if(currFrame < data.seqnr()) 
                                buffer.writeBytes( new byte[253 * (data.seqnr() - currFrame) ] );

                            buffer.writeBytes(data.data(), 0, Integer.min(253, buffer.writableBytes()));
                            currFrame = data.seqnr() + 1;

                            if(currFrame == totalFrames)
                                pushFrame();
                        } catch(NullPointerException ex) {}
                    }
                    
                } catch (IOException ex) {
                    Logger.getLogger(SpectrometerClientMavlink.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        private void pushFrame() {
            if( buffer.readableBytes() == 0) //Buffer is clean, nothing to push
                return;
            
            if( buffer.writableBytes() > 0 ) //We've lost some fragments in the end
                buffer.writeBytes(new byte[buffer.writableBytes()]);
            
            try {
                client.pushMessage(
                        new FrameMessage(buffer.capacity() - headerSize, Message.Type.RAW_FRAME, buffer)
                );
            } catch (Exception ex) {
                Logger.getLogger(SpectrometerClientMavlink.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        private final SpectrometerClientMavlink client;
        private ByteBuf buffer;
        private int currFrame, totalFrames;
    }
    
    private final SerialPort serPort;
    private MavlinkConnection connection;
    private final MavlinkThread thread;
    
    private class SerialInputStream extends InputStream implements SerialPortEventListener {
        
        public SerialInputStream(SerialPort serPort) throws SerialPortException {
            this.serPort = serPort;
            this.serPort.addEventListener(this, SerialPortEvent.RXCHAR);
        }
        
        @Override
        public int read() throws IOException {
            synchronized(sync) {
                try {
                    
                    if(avalBytes <= 0) sync.wait();
                    avalBytes -= 1;
                    return serPort.readBytes(1)[0] & 0xFF;

                } catch (SerialPortException|InterruptedException ex) {
                    throw new IOException(ex);
                }
            }
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            synchronized(sync) {
                if (event.isRXCHAR() && event.getEventValue() > 0) {
                    avalBytes += event.getEventValue();
                    sync.notify();
                }
            }
        }
        
        @Override
        public int available() throws IOException {
            return avalBytes;
        }
        
        private volatile Integer avalBytes = 0;
        private final Object sync = new Object();
        private final SerialPort serPort;
    }
    
    private class DummyOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            //So dummy
        }
    
    }
}