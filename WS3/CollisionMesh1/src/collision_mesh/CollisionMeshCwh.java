package collision_mesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

public class CollisionMeshCwh implements GamePhaseHandler {
	
	private int xDim, yDim;
	private double cellSize;
	private Set<ShapeCwh>[][] cellOccupancyArray;
	private Set<CellId> cellCollisionSet;
	
	private Set<ShapeCwh> allShapeSet = new HashSet<>();
	private Set<ShapeOverlapFact> shapeOverlapFacts = new HashSet<>();
	
	GamePhaseDriver gamePhaseDriver;
	double fpsAvg = 10.0;
	
	
	private static final int XDIM = 1000;
	private static final int YDIM = 1000;
	private static final double CELL_SIZE = 1.0;
	
	
	//LookUp Tables for Sine and CoSine functions (optimization)
	public static final int DEGREE_DIVISIONS = 100;
	public static final double[][] sinTable = new double[360][DEGREE_DIVISIONS];
	public static final double[][] cosTable = new double[360][DEGREE_DIVISIONS];
	static
	{
		for (int degree=0; degree < 360; degree++)
		{
			//Math.cos(Math.toRadians(angleDegrees))*radius;
			for (int division=0; division < DEGREE_DIVISIONS; division++)
			{
				double angleInDegrees = degree + ((double)division)/DEGREE_DIVISIONS;
				sinTable[degree][division] = Math.sin(Math.toRadians(angleInDegrees));
				cosTable[degree][division] = Math.cos(Math.toRadians(angleInDegrees));
			}
		}
		
	}//static
	
	
	private
	class CellId
	{
		int x,y;
		
		public
		CellId(int x, int y)
		{
			assert(x >= 0 && x < xDim/cellSize && y >= 0 && y < yDim/cellSize);
			
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean
		equals(Object other)
		{
			if (!(other instanceof CellId)) return false;
			
			CellId otherCellId = (CellId)other;
			
			return (otherCellId == this || (otherCellId.x == x && otherCellId.y == y));
			
		}
		
		@Override
		public int
		hashCode()
		{
			return x*100_000 + y;
		}
		
		
		@Override
		public String
		toString()
		{
			return "CellId["+x+"]["+y+"]";
		}
	}
	
	
	private static
	class ShapeOverlapFact
	{
		ShapeCwh shapeA, shapeB;
		
		public
		ShapeOverlapFact(ShapeCwh shapeA, ShapeCwh shapeB)
		{
				
			this.shapeA = shapeA;
			this.shapeB = shapeB;
		}
		
		@Override
		public boolean
		equals(Object other)
		{
			if (!(other instanceof ShapeOverlapFact)) return false;
			
			ShapeOverlapFact otherShapeOverlapFact = (ShapeOverlapFact)other;
			
			return (otherShapeOverlapFact == this || 
					(otherShapeOverlapFact.shapeA == shapeA && otherShapeOverlapFact.shapeB == shapeB) ||
					(otherShapeOverlapFact.shapeA == shapeB && otherShapeOverlapFact.shapeB == shapeA));
			
		}
		
		@Override
		public int
		hashCode()
		{
			return shapeA.hashCode() + shapeB.hashCode();
		}
		
		
		@Override
		public String
		toString()
		{
			return "this.hashCode(): "+this.hashCode()+" ShapeA.hashCode(): "+shapeA.hashCode()+" ShapeB.hashCode(): "+shapeB.hashCode();
		}
	}
	
	
	

	
	public
	CollisionMeshCwh(int xDim, int yDim, double cellSize)
	{
		assert(xDim > 0);
		assert(yDim > 0);
		assert(cellSize > 0.0);
		
		this.xDim = xDim;
		this.yDim = yDim;
		this.cellSize = cellSize;
		
		cellOccupancyArray = new HashSet[this.xDim/(int)cellSize][this.yDim/(int)cellSize]; //can't use generic types as array elements
																  //remember only to use with ShapeCwh
		cellCollisionSet = new HashSet<CellId>();
	
		
	}//CollisionMesh()
	
	public
	CollisionMeshCwh(int xDim, int yDim)
	{
		this(xDim, yDim, CELL_SIZE);
	}
	
	public
	CollisionMeshCwh()
	{
		this(XDIM, YDIM, CELL_SIZE);
	}
	
	public void
	recalculate()
	{
		//recalculates structures derived from allShapeSet, which it does NOT touch
		
		//cellOccupancyArray = new HashSet[this.xDim/(int)cellSize][this.yDim/(int)cellSize];
		for (int i=0; i < this.xDim/(int)cellSize; i++)
		for (int j=0; j < this.yDim/(int)cellSize; j++)
		{
			Set s = cellOccupancyArray[i][j];
			if (s != null) s.clear();
		}
		//cellCollisionSet = new HashSet<CellId>();
		cellCollisionSet.clear();
		//shapeOverlapFacts = new HashSet<ShapeOverlapFact>();
		shapeOverlapFacts.clear();
		
		for (ShapeCwh s : allShapeSet)
		{
			registerCirclePerimeterPointsWithCells(s, s.x, s.y, s.r);
		}
		
		

		for (CellId c : this.cellCollisionSet)
		{
			//System.out.println("Member: "+c);
			
			Set<ShapeCwh> shapeSet = this.cellOccupancyArray[c.x][c.y];
			for (ShapeCwh s1 : shapeSet)
			{
				for (ShapeCwh s2 : shapeSet)
				{
					if (s1.equals(s2)) continue;
					
					double distance = Point2D.distance(s1.x, s1.y, s2.x, s2.y);
					if (distance > s1.r + s2.r) continue;
					
					//Shapes overlap...so register that fact
					ShapeOverlapFact sof = new ShapeOverlapFact(s1, s2);
					this.shapeOverlapFacts.add(sof);
					
					//System.out.println("main():4 SOF: " + sof);
				}
			}
		
			//Set<ShapeCwh> shapeSet = collisionMesh.cellOccupancyArray[c.x][c.y];
			//System.out.println("ShapeSet: "+shapeSet);
			//for (ShapeCwh s : shapeSet)
			//{
				//System.out.println(s);
			//}
		}
		
		
	}//recalculate()
	
	
	private void
	registerShapeWithCell(ShapeCwh shape, double xPos, double yPos)
	{
		int x = (int)(xPos/cellSize);
		int y = (int)(yPos/cellSize);
		
//System.out.println("registerShapeWithCell():1 xPos: "+xPos+" yPos: "+yPos+" x: "+x+" y: "+y);
		
		assert(x >= 0 && x < xDim && y >= 0 && y < yDim);
		assert(shape != null);
		
		Set shapeSet = null;
		try
		{
			shapeSet = cellOccupancyArray[x][y];
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			return;
		}
		
		if (shapeSet == null)
		{
			shapeSet = cellOccupancyArray[x][y] = new HashSet<ShapeCwh>();
		}
		
		
		shapeSet.add(shape);
		
		
		
		if (shapeSet.size() == 2)
		{
			cellCollisionSet.add(new CellId(x,y));
//stem.out.println("registerShapeWIthCell():2 adding CellId to CollisionSet: x="+x+" y="+y+" cellCollisionSet.size()="+cellCollisionSet.size());
		}
		
	}//registerShapeWithCell()
	

	public final void
	registerCirclePerimeterPointsWithCells(ShapeCwh shape, final double xPos, final double yPos, double radius)
	{
		
//System.out.println("registerCirclePerimeterPointsWithCells():0 xPos="+xPos+" yPos="+yPos+" radius="+radius);

		double angleIncDegrees = 10.0;
		
		//Trim angleIncDegrees to where we start duping CellIds
		long stime = System.nanoTime();
		
		boolean dupsFound = false;
		do
		{
			int numDups = 0;
			Set<CellId> cellCollisionSet = new HashSet<>();
			CellId prevCellId = new CellId(-111111,-111111);
			for (double angleDegrees = 0.0; angleDegrees < 30.0 && !dupsFound; angleDegrees += angleIncDegrees)
			{
				//double perimeterPointX = xPos + Math.cos(Math.toRadians(angleDegrees))*radius;
				//double perimeterPointY = yPos + Math.sin(Math.toRadians(angleDegrees))*radius;
				int degreeInx = (int)angleDegrees;
				int divisionInx = (int)(angleDegrees*DEGREE_DIVISIONS) % DEGREE_DIVISIONS;
				double perimeterPointX = xPos + cosTable[degreeInx][divisionInx]*radius;
				double perimeterPointY = yPos + sinTable[degreeInx][divisionInx]*radius;
				int x = (int)(perimeterPointX/cellSize);
				int y = (int)(perimeterPointY/cellSize);
				CellId curCellId = new CellId(x,y);
				if (curCellId.equals(prevCellId))
				{
					dupsFound = true;
				}
				else
				{
					prevCellId = curCellId;
				}
			}//for
			
			if (!dupsFound) angleIncDegrees -= 0.50;
		
		} while(!dupsFound);
		
	long etime = System.nanoTime();
	long dtime = etime - stime;
		//stem.out.println("registerCirclePerimeterPointsWithCells():1 dtime: "+dtime);
	//em.out.println("registerCirclePerimeterPointsWithCells():1 dtime: "+dtime);
	//System.out.println("registerCirclePerimeterPointsWithCells():1 dtime: "+dtime);
		
		//Use angleIncDegrees to sensitize the whole perimeter
	angleIncDegrees /= 5; //ensure coverage hack
	
long rTime = 0;
long fTime = 0;
long aTime, bTime;

		for (double angleDegrees = 0.0; angleDegrees < 360.0; angleDegrees += angleIncDegrees)
		{
//aTime = System.nanoTime();
			//double perimeterPointX = xPos + Math.cos(Math.toRadians(angleDegrees))*radius;
			//double perimeterPointY = yPos + Math.sin(Math.toRadians(angleDegrees))*radius;
			int degreeInx = (int)angleDegrees;
			int divisionInx = (int)(angleDegrees*DEGREE_DIVISIONS) % DEGREE_DIVISIONS;
			double perimeterPointX = xPos + cosTable[degreeInx][divisionInx]*radius;
			double perimeterPointY = yPos + sinTable[degreeInx][divisionInx]*radius;
			
			if (perimeterPointX < 0 || perimeterPointY < 0)
			{
				//System.out.println("bad1");
			}
//bTime = System.nanoTime();
//fTime += bTime - aTime;
			
aTime = System.nanoTime();
//System.out.println("registerCirclePerimeterPointsWithCells():1 perimeterPointX="+perimeterPointX+" perimeterPointY="+perimeterPointY);
//System.out.println("registerCirclePerimeterPointsWithCells():2 xPos="+xPos+" yPos="+yPos+" radius="+radius+" degreeInx="+degreeInx+" divisionInx="+divisionInx);
			registerShapeWithCell(shape, perimeterPointX, perimeterPointY);
bTime = System.nanoTime();
			
rTime += bTime - aTime;


			
			
		}//for
		
		etime = System.nanoTime();
		dtime = etime - stime;
		//System.out.println("registerCirclePerimeterPointsWithCells():1 dtime: "+dtime+" rTime: "+rTime+" fTime: "+fTime);
		
		
		
	}
	
	static private long
	nanoMark(String msg)
	{
		long nTime = System.nanoTime();
		System.out.println("Ntime: "+nTime+" msg: "+msg);
		
		return nTime;
	}

	public static void 
	main(String[] args) 
	{
		CollisionMeshCwh collisionMesh = new CollisionMeshCwh(800,500,10);
		
		long aTime,bTime,dTime,sumTime;
		sumTime = 0;
		
		
		
		for (int i = 0; i < 2222; i++)
		{
			//nanoMark("main:1 i="+i);
			double x = collisionMesh.xDim * Math.random();
			double y = collisionMesh.yDim * Math.random();
			double r =  0.2 + 1.0 * Math.random();
			
			
			//collisionMesh.registerCirclePerimeterPointsWithCells(new ShapeCwh(), 500+i,500+i,50);
			aTime = System.nanoTime();
			ShapeCwh s = new ShapeCwh(null, x,y,r, 88*Math.random(), 88*Math.random(), 1, 0);
			collisionMesh.allShapeSet.add(s);
			/*
			collisionMesh.registerCirclePerimeterPointsWithCells(s, x,y,r);
			bTime = System.nanoTime();
			dTime = bTime - aTime;
			
			if (dTime < 100000) {
			sumTime += dTime = bTime - aTime;
			
			//stem.out.println("main():2 dTime: "+dTime+" i: "+i);
			}
			else {
			//ystem.out.println("main():3 dTime: "+dTime+" i: "+i);
			}
			*/
		}
		
		collisionMesh.recalculate();
		
		
			
		
			
			GamePhaseHandler gpHandler = collisionMesh;
			collisionMesh.gamePhaseDriver = new GamePhaseDriver(gpHandler, collisionMesh.xDim, collisionMesh.yDim, Color.GREEN);
			
			long loops = 0;
			
			
			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.setBounds(0, 0, collisionMesh.xDim+10, collisionMesh.yDim+27);
			
			frame.add(collisionMesh.gamePhaseDriver, BorderLayout.CENTER);
			frame.setVisible(true);
			
			collisionMesh.gamePhaseDriver.startGame();
			
			while (!collisionMesh.gamePhaseDriver.gameOver)
			{
				loops++;
			}
		

	}//main()

	long prevUpdateTime, curUpdateTime, updateDeltaTime;
	
	@Override
	public void 
	updateGame() 
	{
		if (prevUpdateTime == 0)
		{
			prevUpdateTime = System.nanoTime();
			return;
		}
		
		long aTime = System.nanoTime();
		curUpdateTime = System.nanoTime();
		updateDeltaTime = curUpdateTime - prevUpdateTime;
		double updateDeltaSecs = (double)updateDeltaTime/1_000_000_000L;
		
		
		for (ShapeCwh s : allShapeSet)
		{
			double xDelta = s.xVel * updateDeltaSecs;
			double yDelta = s.yVel * updateDeltaSecs;
			double xNew = s.x + xDelta;
			double left = xNew - s.r;
			double right = xNew + s.r;
			/*
			if (left < 0 || right > xDim)
			{
				xNew = s.x;
				s.xVel = -s.xVel;
			}
			*/
			if (left < 0)
			{
				s.xVel = s.xVel < 0 ? -s.xVel : s.xVel;
				//System.out.println("updateGame():left<0 s: "+s);
			}
			else
			if (right > xDim)
			{
				s.xVel = s.xVel < 0 ? s.xVel : -s.xVel;
				//System.out.println("updateGame():right>xDim s: "+s);
			}
			s.x = xNew;
			
			double yNew = s.y + yDelta;
			double up = yNew - s.r;
			double down = yNew + s.r;
			/*
			if (up < 0 || down > yDim)
			{
				yNew = s.y;
				s.yVel = -s.yVel;
			}
			*/
			if (up < 0)
			{
				s.yVel = s.yVel < 0 ? -s.yVel : s.yVel;
				//System.out.println("updateGame():up<0 s: "+s);
			}
			else
			if (down > yDim)
			{
				s.yVel = s.yVel < 0 ? s.yVel : -s.yVel;
				//System.out.println("updateGame():down>yDim s: "+s);
			}
			s.y = yNew;
			
			/*
			if (xNew + s.r < xDim && xNew - s.r > 0)
			{
				s.x = xNew;
			}
			if (yNew + s.r < yDim && yNew - s.r > 0)
			{
				s.y = yNew;
			}
			*/
		}
		
		this.recalculate();
		
		
		
		for (ShapeOverlapFact sop : shapeOverlapFacts)
		{
			ShapeCwh sa = sop.shapeA;
			ShapeCwh sb = sop.shapeB;
			
			if (sa.contains(sb) || sb.contains(sa))
			{
				//allShapeSet.remove(sa);
				//allShapeSet.remove(sb);
				
				double mergedX = sa.r >= sb.r ? sa.x : sb.x;
				double mergedY = sa.r >= sb.r ? sa.y : sb.y;
				double mergedXvel = sa.r >= sb.r ? sa.xVel : sb.xVel;
				double mergedYvel = sa.r >= sb.r ? sa.yVel : sb.yVel;
				double mergedArea = Math.PI * (Math.pow(sa.r, 2) + Math.pow(sb.r, 2));
				double mergedRadius = Math.sqrt(mergedArea/Math.PI);
				
				if (mergedXvel == 0 || mergedYvel == 0)
				{
					int i = 3;
				}
				
				if (sa.contains(sb))
				{
					sa.x = mergedX;
					sa.y = mergedY;

					sa.xVel = mergedXvel;
					sa.yVel = mergedYvel;
					sa.r = mergedRadius;
					//System.out.println("updateGame():0.1 sa: " + sa);
					allShapeSet.remove(sb);	
				}
				else
				{
					sb.x = mergedX;
					sb.y = mergedY;
					sb.xVel = mergedXvel;
					sb.yVel = mergedYvel;
					sb.r = mergedRadius;
					//System.out.println("updateGame():0.2 sb: " + sb);
					allShapeSet.remove(sa);
				}
			}
			
		}
		
		
		long bTime = System.nanoTime();
		long dTime = bTime - aTime; 
		
		//System.out.println("updateGame():1 dTime="+dTime);
		
		prevUpdateTime = curUpdateTime;
		
	}
	
	private int renderLoops = 0;

	@Override
	public void 
	renderGame(Image image) 
	{
long aTime = System.nanoTime();
		
		renderLoops++;
		
		// TODO Auto-generated method stub
		Graphics dbg = image.getGraphics();
		
		
		
		
		//Clear background
		dbg.setColor(Color.WHITE);
		dbg.fillRect(0, 0, xDim, yDim);
		
		//Draw game elements
		//....
		
		dbg.setColor(Color.BLACK);
		//dbg.fillOval(10,10,(renderLoops*2) % 400, (renderLoops*2) % 400);
		
		
		/*
		if (gameOver)
		{
			gameOverMessage(dbg);
		}
		*/
		
		/*
		for (CellId c : this.cellCollisionSet)
		{
			//System.out.println("Member: "+c);
		
			Set<ShapeCwh> shapeSet = this.cellOccupancyArray[c.x][c.y];
			//System.out.println("ShapeSet: "+shapeSet);
			for (ShapeCwh s : shapeSet)
			{
				//System.out.println(s);
				
				dbg.drawOval((int)s.x -, (int)s.y, (int)s.r, (int)s.r);
			}//for
		}//for
		*/
		
		/*
		dbg.setColor(Color.ORANGE);
		for (CellId c : this.cellCollisionSet)
		{
			double x = c.x*this.cellSize;
			double y = c.y*this.cellSize;
			
			dbg.fillRect((int)x, (int)y, (int)this.cellSize, (int)this.cellSize);	
			//dbg.drawString(""+x+","+y, (int)x, (int)y);
		}//for
		*/
		
		/*
		dbg.setFont(new Font("Terminal",0,8));
		dbg.setColor(Color.CYAN);
		
		for (int x=0; x < this.xDim/this.cellSize; x++)
		{
			for (int y=0; y < this.yDim/this.cellSize; y++)
			{
				String s = ""+x+":"+y;
				dbg.drawString(s, (int)(x*this.cellSize)+10, (int)(y*this.cellSize)+10);
				
				double xDoub = x * this.cellSize;
				double yDoub = y * this.cellSize;
				dbg.drawRect((int)xDoub,  (int)yDoub, (int)this.cellSize, (int)this.cellSize);
			}
		}
		*/
		
		//Draw count of shapes
		dbg.setColor(Color.BLUE);
		dbg.setFont(new Font("Terminal",0,18));
		dbg.drawString("Shapes: "+allShapeSet.size(), 50, 50);
		fpsAvg =(99*fpsAvg + gamePhaseDriver.fps)/100;
		dbg.drawString("FPS: "+fpsAvg, 50, 100);
		
		dbg.setColor(Color.BLACK);
		for (ShapeCwh s : allShapeSet)
		{
			dbg.drawOval((int)(s.x-s.r), (int)(s.y-s.r), (int)(s.r*2), (int)(s.r*2));
		}
		
		/*
		dbg.setColor(Color.RED);
		for (ShapeOverlapFact sop : shapeOverlapFacts)
		{
			ShapeCwh sa = sop.shapeA;
			ShapeCwh sb = sop.shapeB;
			
			dbg.drawOval((int)(sa.x-sa.r), (int)(sa.y-sa.r), (int)(sa.r*2), (int)(sa.r*2));
			dbg.drawOval((int)(sb.x-sb.r), (int)(sb.y-sb.r), (int)(sb.r*2), (int)(sb.r*2));
			
			//allShapeSet.remove(sa);
			//allShapeSet.remove(sb);
		}
		*/
		
long bTime = System.nanoTime();
long dTime = bTime - aTime;
//System.out.println("renderGame:22 dTime="+dTime);
		
	}

}//class CollisionMeshCwh
