/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.rsce.cansat.granum.spectrometer.client.netty;

import ru.rsce.cansat.granum.spectrometer.client.netty.Message;

/**
 *
 * @author Kirs
 */
public abstract class SpectrometerClient {
    
    public abstract void start() throws Exception;
    public abstract void waitExit() throws Exception;
    
    public static interface ClientMessageListener {
		public void onSpectrometerMessage(Message msg) throws Exception;
    }
    
    public void setMsgListener(ClientMessageListener listener_) {
            msgListener = listener_;
    }


    public void pushMessage(Message msg) throws Exception {
            if (msgListener != null)
                    msgListener.onSpectrometerMessage(msg);
    }
    
    private ClientMessageListener msgListener;
}
