package collision_mesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;




public class 
GamePhaseDriver extends JPanel implements Runnable, GamePhaseHandler
{
	private static final int PWIDTH = 1000;	//size of panel
	private static final int PHEIGHT = 1000;
	
	int pWidth;
	int pHeight;
	Color bgColor;
	
	private Thread animator;		//for the animation
	private GamePhaseHandler gpHandler;
	private volatile boolean running = false;	//stops the animation
	
	volatile boolean gameOver = false;	//for game termination
	
	static int loops;
	
	public double fps;
	
	//More variables, explained later
	//
	
	public
	GamePhaseDriver(GamePhaseHandler gpHandler, int pWidth, int pHeight, Color bgColor) 	//Constructor
	{
		//Just implement Handler side if gpHandler == null
		if (gpHandler == null) return;
		
		//gpHhandler not null, so we are the gpDriver
		this.gpHandler = gpHandler;
		this.pWidth = pWidth;
		this.pHeight = pHeight;
		this.bgColor = bgColor;
		
		setBackground(bgColor);
		setPreferredSize(new Dimension(pWidth, pHeight));
		

		
		
		setFocusable(true);
		setVisible(true);
		requestFocus();
		readyForTermination();
		
		//Create game components
		//.........
		
		//Listen for mouse presses
		addMouseListener
		(
			new
			MouseAdapter()
			{
				public void
				mousePressed(MouseEvent e)
				{
					testPress(e.getX(), e.getY());
				}
			}
		);
		
		
	}//GamePanelOne()
	
	
	private void
	testPress(int x, int y)
	{
		if (gameOver) return;
		
		System.out.println("testPress():1  x="+x+" y="+y);	
		
	}//testPress()
	
	
	private void
	readyForTermination()
	{
		addKeyListener
		(
			new
			KeyAdapter()
			{
				public void
				keyPressed(KeyEvent e)
				{
					int keyCode = e.getKeyCode();
					
					if (
						keyCode == KeyEvent.VK_ESCAPE ||
						keyCode == KeyEvent.VK_Q ||
						keyCode == KeyEvent.VK_END ||
						((keyCode == KeyEvent.VK_C) && e.isControlDown())
					)
					{
						running = false;
					}
				}
				
			}
		);
	}//readyForTerminator()
	
	
	public void
	addNotify()		//Wait for the JPanel to be added to the JFrame/JApplet before starting
	{
		System.out.println("addNotify():1");
		
		super.addNotify();	//Does inherited work
		startGame();		//Does extra work of starting animator thread running in this GamePanelOne obj.
		
	}//addNotify()
	
	
	public void
	startGame()
	{
		if (gpHandler == null)  return;
		
		//First time thru setup code
				if (dbImage == null)
				{
					
					
					dbImage = createImage(pWidth, pHeight);
					if (dbImage == null)
					{
						System.out.println("gameRender():1 dbImage is null");
						System.out.println(GraphicsEnvironment.isHeadless());
						System.exit(-1);
					}
					
					dbg = dbImage.getGraphics();
				
				}//if (dbImage == null)
				
		
		
		if (animator == null || !running)
		{
			animator = new Thread(this);
			animator.start();
		}
	}//startGame()
	
	
	public void
	stopGame()
	{
		running = false;
		
	}//stopGame()
	
	
	private static long GAMELOOP_PERIOD_MS = 100;
	private static long GAMELOOP_PERIOD_NS = GAMELOOP_PERIOD_MS * 1_000_000;

	@Override
	public void 
	run() 			//Update, render, sleep, repeat
	{
		long beforeTime, afterTime, afterTime1, timeDiff, sleepTime;
		
		beforeTime = System.currentTimeMillis();
		
		running = true;
		
		while(running)
		{
			beforeTime = System.nanoTime();
			
			gpHandler.updateGame();		//game state is updated
			gpHandler.renderGame(dbImage);		//render game representation to a buffer
			
			
			//repaint();			//paint the buffer to the screen
			paintScreen();
			
		
			
			
			
			
			
			
			
			try
			{
				//System.out.println("sleepTime: "+sleepTime);
				//Thread.sleep(sleepTime/1_000_000, (int)(sleepTime % 1_000_000) );	//sleep 20 ms...limits FPS to 50
				Thread.sleep(20);
			}
			catch(InterruptedException ex){System.out.printf("Sleep interrupted in GamePhaseDriver.");}
			
			
			
			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			fps = (double)1_000_000_000/timeDiff;
			System.out.println("run():1 timeDiff: "+timeDiff+" fps: "+fps);
			sleepTime = GAMELOOP_PERIOD_NS - timeDiff;
			if (sleepTime <= 0) sleepTime = 5000;
			
		}//while(running)
		
		System.exit(0);			//Game Over...kill this JVM process and thus our app
			

		
	}//run()
	
	
	private void
	paintScreen()
	{
		Graphics g;
		
		try
		{
			g = this.getGraphics();
			if ((g != null) && (dbImage != null))
			{
				g.drawImage(dbImage,  0, 0, null);
				Toolkit.getDefaultToolkit().sync();
			}
		}
		catch (Exception e)
		{
			System.out.println("Graphics context error: " + e);
		}
	}
	
	
	//instance variables for off-screen rendering
	private Graphics dbg;
	private Image dbImage = null;
	private int renderLoops;
	
	public void
	renderGame(Image image)		//draw the current frame to an image buffer
	{
		Graphics dbg = image.getGraphics();
		++renderLoops;
		
		
		
		//Clear background
		dbg.setColor(Color.RED);
		dbg.fillRect(0, 0, pWidth, pHeight);
		
		//Draw game elements
		//....
		
		dbg.setColor(Color.BLACK);
		dbg.fillOval(10,10,(renderLoops*2) % 400, (renderLoops*2) % 400);
		
		if (gameOver)
		{
			gameOverMessage(dbg);
		}
		
			
	}//gameRender()
	
	
	private void
	gameOverMessage(Graphics g)
	{
		int x,y;	//message coordinates
		String msg = "Game Over, Loser!";
		
		x = y = 55;
		
		//Code to calculate x and y
		//.........
		
		g.drawString(msg, x, y);
		
	}
	
	public void
	updateGame()
	{
		//TODO
	}
	
	public void
	paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		if (dbImage != null) 
		{
			g.drawImage(dbImage, 0, 0, null);
		}
	}
	
	public static void
	main(String[] args)
	{
	
		
		System.out.println("Main(): entered");
		
		GamePhaseHandler gpHandler = new GamePhaseDriver(null, 0,0,Color.BLACK);
		GamePhaseDriver gamePhaseDriver = new GamePhaseDriver(gpHandler, PWIDTH, PHEIGHT, Color.GREEN);
		
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, PWIDTH, PHEIGHT);
		
		frame.add(gamePhaseDriver, BorderLayout.CENTER);
		frame.setVisible(true);
		
		gamePhaseDriver.startGame();
		
		while (!gamePhaseDriver.gameOver)
		{
			loops++;
			
		}
	}//main()

} //class GamePhaseDriver

