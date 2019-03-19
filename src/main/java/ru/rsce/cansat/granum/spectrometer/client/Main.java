package ru.rsce.cansat.granum.spectrometer.client;

import java.awt.EventQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ru.rsce.cansat.granum.spectrometer.client.gui.MainWindow;
import ru.rsce.cansat.granum.spectrometer.client.mavlink.SpectrometerClientMavlink;
import ru.rsce.cansat.granum.spectrometer.client.netty.SpectrometerClient;
import ru.rsce.cansat.granum.spectrometer.client.netty.SpectrometerClientNetty;

public class Main {
        public static FrameMessageProcessor msgprocessor; //@FIXME костыльненько как-то
	static Options opts = new Options();
	static HelpFormatter helpFormatter = new HelpFormatter();
	static String cmdLineSyntax = "java -jar spectrometer-client.jar";
	static {
		opts.addOption("h", "host", true, "IP сервера спектрометра");
		opts.addOption("p", "port", true, "порт сервера спектрометра");
                opts.addOption("sp", "serialport", true, "Serial порт при использовании PX4FLOW");
                opts.addOption("b", "baudrate", true, "Serial baudrate");
		opts.addOption("xc", "x-center", true, "Центр оси сканироания по X");
		opts.addOption("xw", "x-width", true, "Ширина оси сканирования по X");
		opts.addOption("yb", "y-begin", true, "Верхняя граница зоны сканирования по y");
		opts.addOption("ye", "y-end", true, "Нижняя граница зоны сканирования по y");
		opts.addOption("cm", "color-mode", true, "Цветовой режим изображения: \"colorfull\"/\"grayscale\"");
	}

	public static void main(String[] args) {

		
		CommandLine line;
		try {
			DefaultParser parser = new DefaultParser();
			line = parser.parse(opts, args, false);
		}
		catch (ParseException e)
		{
			System.out.println(String.format("Ошибка в опциях: %s", e.getMessage()));
			System.out.println("Возможные опции:");
			helpFormatter.printHelp(cmdLineSyntax, opts);
			return;
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				realMain(line);
			}
		});

	}

	
	private static void realMain(CommandLine line) {
		final MainWindow window;
		final SpectrometerClient client;
		final FrameMessageProcessor processor;
		
		final String host;
		final int port;
                final String serialport;
                final int serialbaudrate;
		final int xCenter;
		final int xWidth;
		final int yStart;
		final int yStop;
		final FrameMessageProcessor.ImageColorMode mode;		
		
		try
		{
			host = line.getOptionValue("host", "192.168.0.200");
			port = Integer.parseInt(line.getOptionValue("port", "6112"));
			serialport = line.getOptionValue("serialport", "COM10");
                        serialbaudrate = Integer.parseInt(line.getOptionValue("baudrate", "921600"));
                        xCenter = Integer.parseInt(line.getOptionValue("x-center", String.format("%d", 50)));
			xWidth = Integer.parseInt(line.getOptionValue("x-width", String.format("%d", 1)));
			yStart = Integer.parseInt(line.getOptionValue("y-begin", String.format("%d", 0)));
			yStop = Integer.parseInt(line.getOptionValue("y-end", String.format("%d", 0)));
			mode = FrameMessageProcessor.ImageColorMode.fromString(line.getOptionValue("color-mode", "colorfull"));
		}
		catch (ParseException e)
		{
			System.out.println(String.format("Ошибка при разборе опций: %s", e.getMessage()));
			System.out.println("Возможные опции:");
			helpFormatter.printHelp(cmdLineSyntax, opts);
			return;
		}
		
                try {
                    if(line.hasOption("serialport")) 
                    {
                        client = new SpectrometerClientMavlink(serialport, serialbaudrate);
                    }
                    else 
                    {
                        client = new SpectrometerClientNetty(host, port);
                    }
                    
                    processor = new FrameMessageProcessor();
                    msgprocessor = processor;
                    processor.setImageColorMode(mode);

                    window = new MainWindow();
                    processor.attachToMainWindow(window);
                    processor.attachToSpectrometerClient(client);
                    processor.setScanlineParams(xCenter, xWidth, yStart, yStop);
                    
                    client.start();
                    
                } catch (Exception e) {
                            e.printStackTrace();
                            return;
                }

		window.show();
	}
}
