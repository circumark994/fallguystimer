import java.util.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.io.*;
import javax.imageio.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.text.SimpleDateFormat;

class FallGuysTimer extends JFrame {
	static FallGuysTimer frame;
	static timerThread timerthread;
	static PlayerlogThread playerlogthread;
	static Font fontfamily;

	public static void main(String[] args) throws Exception {
		int pt_x = 10;
		int pt_y = 10;
		int size_x = 100;
		int size_y = 100;
		BufferedImage image = null;
		try{
			File file = new File("window_pt.ini");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			String[] value;
			while((str = br.readLine()) != null) {
				value = str.split(" ", 2);
				pt_x = Integer.parseInt(value[0]);
				pt_y = Integer.parseInt(value[1]);
			}
			br.close();

			file = new File("path.ini");
			br = new BufferedReader(new FileReader(file));
			while((str = br.readLine()) != null) {
				path_str = str;
			}
			br.close();

			image = ImageIO.read(new File("background.png"));
			size_x = image.getWidth();
			size_y = image.getHeight();

			fontfamily = Font.createFont(Font.TRUETYPE_FONT,new File("TitanOne-Regular.ttf"));
			fontfamily = fontfamily.deriveFont(30f);

		} catch(FileNotFoundException e) { System.exit(1);
		} catch(FontFormatException e){ System.exit(1);
		} catch(IOException e) { System.exit(1); }
		
		frame = new FallGuysTimer(size_x, size_y, image);
		frame.setUndecorated(true);
		frame.setBounds(pt_x, pt_y, size_x, size_y);
		frame.setTitle("Custom Fall Ball Punch");
		frame.setBackground(new Color(0x0, true));
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
	}

	static JPanel p;
	static String path_str;
	static JLabel timer;
	static boolean timer_flg;
	static Point mouseDownCompCoords;
	private JPopupMenu popup;

	static Date startDate;
	static Date endDate;
	static SimpleDateFormat sdf_utc;

	FallGuysTimer(int size_x, int size_y, BufferedImage image) {
		p = new JPanel(null) {
			@Override
			public void paintComponent(Graphics g) {
				g.drawImage(image, 0, 0, this);
			}
		};
		p.setSize(size_x, size_y);

		timer = new JLabel("  00:00.00");
		timer.setSize(image.getWidth()+100, image.getHeight()-16);
		timer.setHorizontalAlignment(JLabel.LEFT);
		timer.setVerticalAlignment(JLabel.BOTTOM);
		timer.setForeground(Color.WHITE);
		timer.setFont(fontfamily);
		p.add(timer);

		popup = new JPopupMenu();
		JMenuItem popup_start = new JMenuItem("スタート");
		popup_start.setFont(new Font("メイリオ", Font.BOLD, 14));
		popup.add(popup_start);
		popup_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (timer_flg == false){
					startDate = getCurDateUTC();
					timer_flg = true;
				}
			}
		});
		JMenuItem popup_reset = new JMenuItem("リセット");
		popup_reset.setFont(new Font("メイリオ", Font.BOLD, 14));
		popup.add(popup_reset);
		popup_reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (timer_flg == true) {
					endDate = getCurDateUTC();
					timer_flg = false;
				}
			}
		});
		JMenuItem popup_shutdown = new JMenuItem("終了");
		popup_shutdown.setFont(new Font("メイリオ", Font.BOLD, 14));
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
			public void mouseClicked(MouseEvent e) {}
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

		sdf_utc = new SimpleDateFormat("HH:mm:ss.SSS");
		sdf_utc.setTimeZone(TimeZone.getTimeZone("UTC"));

		timerthread = new timerThread();
		timerthread.start();
		playerlogthread = new PlayerlogThread();
		playerlogthread.start();
	}

	private void showPopup(MouseEvent e){
		if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY());
	}
	private void save() {
		try{
		  File file = new File("window_pt.ini");
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
}

class timerThread extends Thread{
	public void run(){
		while (true){
			while (FallGuysTimer.frame.timer_flg == true){
				long diff = caltimer(FallGuysTimer.frame.getCurDateUTC());
				if (diff < 100*60*1000) {
					displayTimer(diff);
					if (FallGuysTimer.frame.timer_flg == false) break;
				} else {
					FallGuysTimer.frame.timer_flg = false;
					break;
				}
				try{
			 		Thread.sleep(10);
				} catch (InterruptedException e) {}
			}
			if ((FallGuysTimer.frame.startDate != null) && (FallGuysTimer.frame.endDate != null)){
				long diff = caltimer(FallGuysTimer.frame.endDate);
				if (diff < 100*60*1000) displayTimer(diff);
			}
			try{
			 	Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}
	
	private long caltimer(Date curDate){
		long startDateMill = FallGuysTimer.frame.startDate.getTime();
		long curDateMill = curDate.getTime();
		long diff = curDateMill - startDateMill;
		if (getHour(startDateMill) > getHour(curDateMill)) diff = diff + 24*60*60*1000;
		return diff;
	}

	private long getHour(long dateMill){
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(dateMill);
		return cal.get(Calendar.HOUR_OF_DAY);
	}

	private void displayTimer(long count){
		if (count == 0){
			FallGuysTimer.frame.timer.setText("  00:00.00");
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
		FallGuysTimer.frame.timer.setText(text);
	}
}

class PlayerlogThread extends Thread{
	private Path log_path;
	private int line_cnt;
	private long file_size;
	static int match_status;
	private SimpleDateFormat sdf_utc;

	public void run() {
		log_path = Paths.get(FallGuysTimer.frame.path_str);
		line_cnt = 0;
		file_size = 0;
		match_status = 0;
		sdf_utc = new SimpleDateFormat("HH:mm:ss.SSS");
		sdf_utc.setTimeZone(TimeZone.getTimeZone("UTC"));

		while (true) {
			long cur_file_size = new File(FallGuysTimer.frame.path_str).length();
			if (file_size > cur_file_size) { line_cnt = 0; match_status = 0; }
			file_size = cur_file_size;

			int tmp_line_cnt = 0;
			try (BufferedReader br = Files.newBufferedReader(log_path, Charset.forName("UTF-8"))) {
				String text;
				while((text = br.readLine()) != null) {
					if (tmp_line_cnt >= line_cnt) {
						getStartTime(text);
					}
					tmp_line_cnt++;
				}
				line_cnt = tmp_line_cnt;
			} catch (Exception e) {}
			try{
			 	Thread.sleep(3*1000);
			} catch (InterruptedException e) {}
		}
	}

	private void getStartTime(String text) {
		switch(match_status) {
			case 0: // load a game
				if (text.indexOf("[StateGameLoading] Loading game level scene") != -1) {
					FallGuysTimer.frame.timer_flg = false;
					FallGuysTimer.frame.startDate = FallGuysTimer.frame.getCurDateUTC();
					FallGuysTimer.frame.endDate = FallGuysTimer.frame.getCurDateUTC();
					match_status = 1;
				}
				break;

			case 1: // start a game
				if (text.indexOf("[GameSession] Changing state from Countdown to Playing") != -1){
					String[] sp = text.split(": ", 2);
					try {
						FallGuysTimer.frame.startDate = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.endDate = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = true;
						match_status = 2;
					} catch (Exception e){}
				} else if ((text.indexOf("[ClientGameManager] Server notifying that the round is over.") != -1) ||
				(text.indexOf("[Hazel] [HazelNetworkTransport] Disconnect request received for connection 0. Reason: Connection disposed") != -1) ||
				(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					match_status = 0;
				}
				break;
			case 2: // end a game
				if ((text.indexOf("[GameSession] Changing state from GameOver to Results") != -1) ||
				(text.indexOf("Cannot cycle spectators when not using the player spectator camera.") != -1) ||
				(text.indexOf("[ClientGameManager] Server notifying that the round is over.") != -1) ||
				(text.indexOf("[Hazel] [HazelNetworkTransport] Disconnect request received for connection 0. Reason: Connection disposed") != -1) ||
				(text.indexOf("[StateMatchmaking] Begin matchmaking") != -1)) {
					String[] sp = text.split(": ", 2);
					try {
						FallGuysTimer.frame.endDate = sdf_utc.parse(sp[0]);
						FallGuysTimer.frame.timer_flg = false;
						match_status = 0;
					} catch (Exception e){}
				}
				break;
		}
	}
}