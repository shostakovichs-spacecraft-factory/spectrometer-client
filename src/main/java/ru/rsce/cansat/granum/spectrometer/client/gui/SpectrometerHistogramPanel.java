/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.rsce.cansat.granum.spectrometer.client.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;

/**
 *
 * @author Kirs
 */
public class SpectrometerHistogramPanel extends JPanel{
    
    public SpectrometerHistogramPanel () {
        histogramData = new HistogramDataset();
        
        chart = ChartFactory.createHistogram(   "Luminosity", "", "", 
                                                histogramData, PlotOrientation.HORIZONTAL, 
                                                true, true, true);
        
        panel = new ChartPanel(chart);
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
    }
    
    public void updateData (byte[] picture) {
        double[] data = new double[256];
        
        for (int pixId = 0; pixId < picture.length / 2; pixId++) {
            int y = picture[pixId * 2] & 0xFF;
            data[y] += 1;
        }
        
        for(double dat:data) {
            System.out.print(dat + " ");
        }
        System.out.println();
        
        histogramData = new HistogramDataset();
        histogramData.addSeries("Luminosity", data, 256);
        
        chart = ChartFactory.createHistogram(   "Luminosity", "", "", 
                                                histogramData, PlotOrientation.HORIZONTAL, 
                                                true, false, false);
        
        panel = new ChartPanel(chart);
        this.removeAll();
        this.add(panel, BorderLayout.CENTER);
    }
    
    HistogramDataset histogramData;
    JFreeChart chart;
    ChartPanel panel;
}
