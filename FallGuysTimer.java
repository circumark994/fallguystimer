import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

class FallGuysTimer extends JFrame {
	static FallGuysTimer frame;
	static Font fontfamily;

	public static void main(String[] args) {
		int pt_x = 10;
		int pt_y = 10;
		int size_x = 100;
		int size_y = 100;
		BufferedImage image = null;
		try{
			File file = new File("./resource/window_pt.ini");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			String[] value;
			while((str = br.readLine()) != null) {
				value = str.split(" ", 2);
				pt_x = Integer.parseInt(value[0]);
				pt_y = Integer.parseInt(value[1]);
			}
			br.close();

			image = ImageIO.read(new File("./resource/background.png"));
			size_x = image.getWidth();
			size_y = image.getHeight();

			fontfamily = Font.createFont(Font.TRUETYPE_FONT,new File("./resource/TitanOne-Regular.ttf"));
			fontfamily = fontfamily.deriveFont(30f);

		} catch(FileNotFoundException e) { System.exit(1);
		} catch(FontFormatException e){ System.exit(1);
		} catch(IOException e) { System.exit(1); }
		
		frame = new FallGuysTimer(size_x, size_y, image);
		frame.setUndecorated(true);
		frame.setBounds(pt_x, pt_y, size_x, size_y);
		frame.setTitle("FallGuysTimer");
		frame.setBackground(new Color(0x0, true));
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
	}

	static JPanel p;
	static JLabel timer;
	static int timer_flg;
	static int timer_disp_flg;
	static Point mouseDownCompCoords;
	private JPopupMenu popup;

	static Date startDate;
	static Date endDate1;
	static Date endDate2;
	static SimpleDateFormat sdf_utc;

	private PlayerlogReader playerlogreader;

	FallGuysTimer(int size_x, int size_y, BufferedImage image) {
		p = new JPanel(null) {
			@Override
			public void paintComponent(Graphics g) {
				g.drawImage(image, 0, 0, this);
			}
		};
		p.setSize(size_x, size_y);
		
		timer_flg = 0; // 0:両方停止, 1:両方動作, 2:片方動作
		timer_disp_flg = 0; // 0:自分のタイマー, 1:ラウンドタイマー
		sdf_utc = new SimpleDateFormat("HH:mm:ss.SSS");
		sdf_utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		startDate = getCurDateUTC();
		endDate1 = startDate;
		endDate2 = startDate;

		timer = new JLabel("  00:00.00");
		timer.setSize(image.getWidth()+100, image.getHeight()-16);
		timer.setHorizontalAlignment(JLabel.LEFT);
		timer.setVerticalAlignment(JLabel.BOTTOM);
		timer.setForeground(Color.WHITE);
		timer.setFont(fontfamily);
		p.add(timer);

		popup = new JPopupMenu();
		JMenuItem popup_start = new JMenuItem("Start");
		popup_start.setFont(new Font("Meiryo UI", Font.BOLD, 16));
		popup.add(popup_start);
		popup_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (timer_flg == 0){
					startDate = getCurDateUTC();
					endDate1 = startDate;
					endDate2 = startDate;
					timer_flg = 1;
					TimerThread timerthread = new TimerThread();
					timerthread.start();
				}
			}
		});
		JMenuItem popup_reset = new JMenuItem("Stop");
		popup_reset.setFont(new Font("Meiryo UI", Font.BOLD, 16));
		popup.add(popup_reset);
		popup_reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (timer_flg != 0) {
					endDate2 = getCurDateUTC();
					if (timer_flg == 1) endDate1 = endDate2;
					timer_flg = 0;
					displayTimer();
				}
			}
		});
		JMenuItem popup_shutdown = new JMenuItem("Close");
		popup_shutdown.setFont(new Font("Meiryo UI", Font.BOLD, 16));
		popup.add(popup_shutdown);
		popup_shutdown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		
		p.addMouseListener(new MouseListener(){
			public void mouseReleased(MouseEvent e) {
				boolean press = SwingUtilities.isRightMouseButton(e);
				if (press == true) showPopup(e);
				mouseDownCompCoords = null;
			}
			public void mousePressed(MouseEvent e) {
				boolean press = SwingUtilities.isLeftMouseButton(e);
				if (press == true) mouseDownCompCoords = e.getPoint();
			}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {
				boolean press = SwingUtilities.isLeftMouseButton(e);
				if ((press == true) && (e.getClickCount() == 2)){
					if (timer_disp_flg == 0) {
						timer_disp_flg = 1;
						timer.setForeground(Color.YELLOW);
						displayTimer();
					} else {
						timer_disp_flg = 0;
						timer.setForeground(Color.WHITE);
						displayTimer();
					}
				}
			}
		});
		p.addMouseMotionListener(new MouseMotionListener(){
			public void mouseMoved(MouseEvent e) {}
			public void mouseDragged(MouseEvent e) {
				boolean press = SwingUtilities.isLeftMouseButton(e);
				if (press == true){
					Point currCoords = e.getLocationOnScreen();
					frame.setLocation(currCoords.x-mouseDownCompCoords.x, currCoords.y-mouseDownCompCoords.y);
				}
			}
		});

		Container contentPane = getContentPane();
		contentPane.add(p, BorderLayout.CENTER);

		playerlogreader = new PlayerlogReader(new File(System.getProperty("user.home") + "/AppData/LocalLow/Mediatonic/FallGuys_client/Player.log"));
		playerlogreader.start();
	}

	private void showPopup(MouseEvent e){
		if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY());
	}
	private void save() {
		try{
		  File file = new File("./resource/window_pt.ini");
		  PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		  Point pt = frame.getLocationOnScreen();
		  pw.print(pt.x + " " + pt.y);
		  pw.close();
		}catch(IOException e) {}
		System.exit(0);
	}

	static Date getCurDateUTC(){
		Date curDate = new Date();
		String curDate_str = sdf_utc.format(curDate);
		try {
			curDate = sdf_utc.parse(curDate_str);
		} catch (Exception e){}
		return curDate;
	}

	static void displayTimer(){
		long diff = 0;
		switch(timer_flg) {
			case 0: // 停止
				if (timer_disp_flg == 0) { diff = calTimer(endDate1);
				} else diff = calTimer(endDate2);
				break;
			case 1: // 両方動作
				diff = calTimer(getCurDateUTC());
				break;
			case 2: // 片方動作
				if (timer_disp_flg == 0) { diff = calTimer(endDate1);
				} else diff = calTimer(getCurDateUTC());
				break;
		}
		if (diff < 100*60*1000) {
			transTimer(diff);
		} else {
			timer_flg = 0; // 強制停止
			timer.setText("  99:59.99");
		}
	}

	static long calTimer(Date curDate){
		long startDateMill = startDate.getTime();
		long curDateMill = curDate.getTime();
		long diff = curDateMill - startDateMill;
		if (startDateMill > curDateMill) diff = diff + 24*60*60*1000;
		return diff;
	}

	static void transTimer(long count){
		if (count == 0){
			timer.setText("  00:00.00");
			return;
		}

		String text = "  ";
		long min = count / (60*1000);
		if (min < 10){
			text = text + "0" + String.valueOf(min) + ":";
		} else text = text + String.valueOf(min) + ":";
		long sec = count % (60*1000);
		long msec = sec % 1000;
		sec = sec / 1000;
		if (sec < 10){
			text = text + "0" + String.valueOf(sec) + ".";
		} else text = text + String.valueOf(sec) + ".";
		if (msec < 10){
			text = text + "00";
		} else if (msec < 100){
			text = text + "0" + String.valueOf(msec/10);
		} else {
			text = text + String.valueOf(msec/10);
		}
		timer.setText(text);
	}
}

class TimerThread extends Thread{
	public void run(){
		while (FallGuysTimer.frame.timer_flg != 0){
			FallGuysTimer.frame.displayTimer();
			if (FallGuysTimer.frame.timer_flg == 0) break;
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}
	}
}

class PlayerlogReader extends TailerListenerAdapter {
	private Tailer tailer;
	private Thread thread;
	private int match_status;
	private SimpleDateFormat sdf_utc;

	public PlayerlogReader(File log) {
		match_status = 0;
		sdf_utc = new SimpleDateFormat("HH:mm:ss.SSS");
		sdf_utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		tailer = new Tailer(log, Charset.forName("UTF-8"), this, 10, false, false, 8192);
	}
	public void start() {
		thread = new Thread(tailer);
		thread.start();
	}

	@Override
	public void handle(String text) {
		try {
			getStartTime(text);
		} catch (Exception e) {}
	}

	public void getStartTime(String text) {
		switch(match_status) {
			case 0: // load a game
				if (text.indexOf("[StateGameLoading] Loading game level scene") != -1) {
					FallGuysTimer.frame.timer_flg = 0;
					FallGuysTimer.frame.startDate = FallGuysTimer.frame.getCurDateUTC();
					FallGuysTimer.frame.endDate1 = FallGuysTimer.frame.startDate;
					FallGuysTimer.frame.endDate2 = FallGuysTimer.frame.startDate;
					match_status = 1;
					FallGuysTimer.frame.displayTimer();
				}
				break;

			case 1: // start a game
				if (text.indexOf("[GameSession] Changing state from Countdown to Playing") != -1){
					String[] sp = text.split(": ", 2);
					try {
						FallGuysTimer.frame.startDate = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.endDate1 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.endDate2 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = 1;
						match_status = 2;
						TimerThread timerthread = new TimerThread();
						timerthread.start();
					} catch (Exception e){}
				} else if ((text.indexOf("[ClientGameManager] Server notifying that the round is over.") != -1) ||
				(text.indexOf("[Hazel] [HazelNetworkTransport] Disconnect request received for connection 0. Reason: Connection disposed") != -1) ||
				(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					String[] sp = text.split(": ", 2);
					try {
						if (FallGuysTimer.frame.timer_flg == 1) FallGuysTimer.frame.endDate1 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.endDate2 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = 0;
						match_status = 0;
						FallGuysTimer.frame.displayTimer();
					} catch (Exception e){}
				}
				break;

			case 2: // end a game
				if (text.indexOf("Cannot cycle spectators when not using the player spectator camera.") != -1){
					String[] sp = text.split(": ", 2);
					try {
						FallGuysTimer.frame.endDate1 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = 2;
						FallGuysTimer.frame.displayTimer();
					} catch (Exception e){}
				} else if ((text.indexOf("entering crown grab state") != -1) ||
				(text.indexOf("[ClientGameManager] Server notifying that the round is over.") != -1) ||
				(text.indexOf("[Hazel] [HazelNetworkTransport] Disconnect request received for connection 0. Reason: Connection disposed") != -1) ||
				(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					String[] sp = text.split(": ", 2);
					try {
						if (FallGuysTimer.frame.timer_flg == 1) FallGuysTimer.frame.endDate1 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.endDate2 = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = 0;
						match_status = 0;
						FallGuysTimer.frame.displayTimer();
					} catch (Exception e){}
				}
				break;
		}
	}
}
