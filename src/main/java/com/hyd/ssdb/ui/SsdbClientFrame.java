package com.hyd.ssdb.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Swing UI (not finished)
 *
 * @author yiding_he
 */
public class SsdbClientFrame extends JFrame {

    public SsdbClientFrame() throws HeadlessException {
        super("SSDB Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        setupComponents();
    }

    private void setupComponents() {

    }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        new SsdbClientFrame().setVisible(true);
    }
}
