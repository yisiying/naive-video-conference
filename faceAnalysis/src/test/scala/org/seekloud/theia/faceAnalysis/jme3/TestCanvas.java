package org.seekloud.theia.faceAnalysis.jme3;

/**
 * Created by sky
 * Date on 2019/9/18
 * Time at 10:12
 */
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.util.JmeFormatter;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javax.swing.*;

public class TestCanvas {

    private static JmeCanvasContext context;
    private static Canvas canvas;
    private static HelloSkeleton3 app;
    private static JFrame frame;
    private static Container canvasPanel1, canvasPanel2;
    private static Container currentPanel;
    private static JTabbedPane tabbedPane;

    private static void createTabs(){
        tabbedPane = new JTabbedPane();

        canvasPanel1 = new JPanel();
        canvasPanel1.setLayout(new BorderLayout());
        tabbedPane.addTab("jME3 Canvas 1", canvasPanel1);

        canvasPanel2 = new JPanel();
        canvasPanel2.setLayout(new BorderLayout());
        tabbedPane.addTab("jME3 Canvas 2", canvasPanel2);

        frame.getContentPane().add(tabbedPane);

        currentPanel = canvasPanel1;
    }

    private static void createMenu(){
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menuTortureMethods = new JMenu("Canvas Torture Methods");
        menuBar.add(menuTortureMethods);

        final JMenuItem itemRemoveCanvas = new JMenuItem("Remove Canvas");
        menuTortureMethods.add(itemRemoveCanvas);
        itemRemoveCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (itemRemoveCanvas.getText().equals("Remove Canvas")){
                    currentPanel.remove(canvas);

                    itemRemoveCanvas.setText("Add Canvas");
                }else if (itemRemoveCanvas.getText().equals("Add Canvas")){
                    currentPanel.add(canvas, BorderLayout.CENTER);

                    itemRemoveCanvas.setText("Remove Canvas");
                }
            }
        });

        final JMenuItem itemHideCanvas = new JMenuItem("Hide Canvas");
        menuTortureMethods.add(itemHideCanvas);
        itemHideCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (itemHideCanvas.getText().equals("Hide Canvas")){
                    canvas.setVisible(false);
                    itemHideCanvas.setText("Show Canvas");
                }else if (itemHideCanvas.getText().equals("Show Canvas")){
                    canvas.setVisible(true);
                    itemHideCanvas.setText("Hide Canvas");
                }
            }
        });

        final JMenuItem itemSwitchTab = new JMenuItem("Switch to tab #2");
        menuTortureMethods.add(itemSwitchTab);
        itemSwitchTab.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if (itemSwitchTab.getText().equals("Switch to tab #2")){
                    canvasPanel1.remove(canvas);
                    canvasPanel2.add(canvas, BorderLayout.CENTER);
                    currentPanel = canvasPanel2;
                    itemSwitchTab.setText("Switch to tab #1");
                }else if (itemSwitchTab.getText().equals("Switch to tab #1")){
                    canvasPanel2.remove(canvas);
                    canvasPanel1.add(canvas, BorderLayout.CENTER);
                    currentPanel = canvasPanel1;
                    itemSwitchTab.setText("Switch to tab #2");
                }
            }
        });

        JMenuItem itemSwitchLaf = new JMenuItem("Switch Look and Feel");
        menuTortureMethods.add(itemSwitchLaf);
        itemSwitchLaf.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Throwable t){
                    t.printStackTrace();
                }
                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
            }
        });

        JMenuItem itemSmallSize = new JMenuItem("Set size to (0, 0)");
        menuTortureMethods.add(itemSmallSize);
        itemSmallSize.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                Dimension preferred = frame.getPreferredSize();
                frame.setPreferredSize(new Dimension(0, 0));
                frame.pack();
                frame.setPreferredSize(preferred);
            }
        });

        JMenuItem itemKillCanvas = new JMenuItem("Stop/Start Canvas");
        menuTortureMethods.add(itemKillCanvas);
        itemKillCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentPanel.remove(canvas);
                app.stop(true);

                createCanvas();
                currentPanel.add(canvas, BorderLayout.CENTER);
                frame.pack();
                startApp();
            }
        });

        JMenuItem itemExit = new JMenuItem("Exit");
        menuTortureMethods.add(itemExit);
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                frame.dispose();
                app.stop();
            }
        });
    }

    private static void createFrame(){
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosed(WindowEvent e) {
                app.stop();
            }
        });

        createTabs();
        createMenu();
    }

    public static void createCanvas(){
        AppSettings settings = new AppSettings(true);
        settings.setWidth(640);
        settings.setHeight(480);

        try{
            app = HelloSkeleton3.class.newInstance();
        }catch (InstantiationException ex){
            ex.printStackTrace();
        }catch (IllegalAccessException ex){
            ex.printStackTrace();
        }

        app.setPauseOnLostFocus(false);
        app.setSettings(settings);
        app.createCanvas();
        app.startCanvas();

        context = (JmeCanvasContext) app.getContext();
        canvas = context.getCanvas();

        canvas.setSize(settings.getWidth(), settings.getHeight());
    }

    public static void startApp(){
        app.startCanvas();
        app.enqueue(new Callable<Void>(){
            public Void call(){
                if (app instanceof SimpleApplication){
                    SimpleApplication simpleApp = (SimpleApplication) app;
                    simpleApp.getFlyByCamera().setDragToRotate(true);
                }
                return null;
            }
        });

    }

    public static void main(String[] args){
        JmeFormatter formatter = new JmeFormatter();

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);

        Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
        Logger.getLogger("").addHandler(consoleHandler);

        createCanvas();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }

        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                createFrame();

                currentPanel.add(canvas, BorderLayout.CENTER);
                frame.pack();
                startApp();

                app.enqueue(new Runnable() {
                    @Override
                    public void run() {
                        app.mouthChange(0.8f);
                    }
                });

                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

}
