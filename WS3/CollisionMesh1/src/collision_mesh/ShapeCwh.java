package collision_mesh;

import java.awt.Color;
import java.awt.geom.Point2D;

public class 
ShapeCwh 
{
	static int shapeIdNext;
	
	int shapeId;
	CollisionMesh2Cwh mesh;
	int shapeType;
	double x,y,r;
	double xVel, yVel;
	int strategy;	//0 == none, just bounce around with physics
	int splitGeneration;
	ShapeCwh peer;  // for linking shadow shape (for now)
	
	Color color = Color.BLACK;
	
	public
	ShapeCwh(CollisionMesh2Cwh mesh, double x, double y, double r, double xVel, double yVel, int strategy, int splitGeneration)
	{
		this.shapeId = ++shapeIdNext;
		
		this.mesh = mesh;
		
		this.x = x;
		this.y = y;
		this.r = r;
		
		this.xVel = xVel;
		this.yVel = yVel;
		
		this.strategy = strategy;
		this.splitGeneration = splitGeneration;
		if (strategy == 1) color = Color.GREEN;
		if (strategy == 2) color = Color.RED;
		if (strategy == 3) color = Color.BLUE;
		if (strategy == 4) color = Color.ORANGE;
	}
	
	public
	ShapeCwh(CollisionMesh2Cwh mesh, double x, double y, double r)
	{
		this(mesh, x, r, y, 0, 0, 0, 0);
	}
	
	public String
	toString()
	{
		return "ShapeCwh@"+this.hashCode()+" x="+x+" y="+y+" r="+r+
				" xVel="+xVel+" yVel="+yVel+" strategy="+strategy+" splitGeneration="+splitGeneration;
	}
	
	public boolean
	contains(ShapeCwh s2)
	{
		ShapeCwh s1 = this;
		if (s1.equals(s2)) return false;
		
		double distance = Point2D.distance(s1.x, s1.y, s2.x, s2.y);
		return distance + s2.r <= s1.r;
		
		
	}
	
	//ShapeCwh nullShape = new ShapeCwh(this.mesh, 0,0,0,0,0,0);
	
	public boolean
	defensiveStrategy1(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		
		ShapeCwh nullShape = new ShapeCwh(this.mesh, 0,0,0,0,0,0,0);
		ShapeCwh killerShape =  nullShape;
		for (CollisionMesh2Cwh.ShapeOverlapFact sof : mesh.shapeOverlapFacts)
		{
			if (this == sof.shapeA && sof.shapeB.r > this.r && sof.shapeB.r > killerShape.r)
			{
				killerShape = sof.shapeB;
			}
			else
			if (this == sof.shapeB && sof.shapeA.r > this.r && sof.shapeA.r > killerShape.r)
			{
				killerShape = sof.shapeA;
			}
		}//for
	
		if (killerShape != nullShape)
		{
			this.xVel += (killerShape.x < this.x) ? 5.0 : -5.0;
			this.yVel += (killerShape.y < this.y) ? 5.0 : -5.0;
		}
	
		return killerShape != nullShape;
		
	}//defensiveStrategy1()
	
	
	public boolean
	defensiveStrategy4(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		
		ShapeCwh nullShape = new ShapeCwh(this.mesh, 0,0,0,0,0,0,0);
		ShapeCwh killerShape =  nullShape;
		for (CollisionMesh2Cwh.ShapeOverlapFact sof : mesh.helperMesh.shapeOverlapFacts)
		{
			if (this == sof.shapeA.peer && sof.shapeB.peer.r > this.r && sof.shapeB.peer.r > killerShape.r)
			{
				killerShape = sof.shapeB.peer;
			}
			else
			if (this == sof.shapeB.peer && sof.shapeA.peer.r > this.r && sof.shapeA.peer.r > killerShape.r)
			{
				killerShape = sof.shapeA.peer;
			}
		}//for
	
		if (killerShape != nullShape)
		{
			this.xVel += (killerShape.x < this.x) ? 5.0 : -5.0;
			this.yVel += (killerShape.y < this.y) ? 5.0 : -5.0;
		}
	
		return killerShape != nullShape;
		
	}//defensiveStrategy4()
	
	
	public boolean
	offensiveStrategy1(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		
		//Nothing to worry about, go for victims
		ShapeCwh victimShape = null;
		for (CollisionMesh2Cwh.ShapeOverlapFact sof : mesh.shapeOverlapFacts)
		{
		
		
			if (this == sof.shapeA && sof.shapeB.r < this.r && (victimShape == null || sof.shapeB.r > victimShape.r))
			{
				victimShape = sof.shapeB;
			}
			else
			if (this == sof.shapeB && sof.shapeA.r < this.r && (victimShape == null || sof.shapeA.r > victimShape.r))
			{
				victimShape = sof.shapeA;
			}
		}//for
	
		if (victimShape != null)
		{
			
			this.x = victimShape.x;
			this.y = victimShape.y;   
			
		}
			
			
		
		return false;
	}
	
	public boolean
	offensiveStrategy4(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		return offensiveStrategy1(mesh, updateDeltaNanos);
	}
	
	final int MAX_SPLIT_GENERATIONS = 1;
	final double SHOT_VELOCITY_BASE = 16.0;
	final double SHOT_LEAP_SECS = 1.55;
	
	public boolean
	offensiveStrategy2(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		
		//Nothing to worry about, go for victims
		ShapeCwh victimShape = null;
		for (CollisionMesh2Cwh.ShapeOverlapFact sof : mesh.shapeOverlapFacts)
		{
		
		
			if (this == sof.shapeA && sof.shapeB.r < this.r && sof.shapeB.strategy != this.strategy && (victimShape == null || sof.shapeB.r > victimShape.r))
			{
				victimShape = sof.shapeB;
			}
			else
			if (this == sof.shapeB && sof.shapeA.r < this.r && sof.shapeA.strategy != this.strategy && (victimShape == null || sof.shapeA.r > victimShape.r))
			{
				victimShape = sof.shapeA;
			}
		}//for
	
		if (victimShape != null && victimShape.strategy != this.strategy && victimShape.r < this.r/Math.sqrt(2) && this.splitGeneration < MAX_SPLIT_GENERATIONS)
		{
			// Split this ball and launch baby at victimShape
			this.r /= Math.sqrt(2);
			this.splitGeneration++;
			
			// Make clone ball
			int shotXdir = (victimShape.x < this.x) ? -1 : 1;
			int shotYdir = (victimShape.y < this.y) ? -1 : 1;
			
			double absSlope = (victimShape.y - this.y)/(victimShape.x - this.x);
			if (absSlope < 0) absSlope = -absSlope;
			
			final double shotVelBase = SHOT_VELOCITY_BASE;
			double shotXvel = (shotXdir == 1) ? shotVelBase : -shotVelBase;
			double shotYvel = ((shotYdir == 1) ? shotVelBase : -shotVelBase) * absSlope;
			
			// time-leap a fraction of a second to advance shot
			double shotXdisp = shotXvel * SHOT_LEAP_SECS;
			double shotYdisp = shotYvel * SHOT_LEAP_SECS;
			
			
			
			
			
			mesh.allShapeSet.add(new ShapeCwh(mesh, this.x + shotXdisp, this.y + shotYdisp, this.r, shotXvel, shotYvel, this.strategy, this.splitGeneration));
			
		}
			
			
		
		return false;
	}
	
	final double VELOCITY_RETARD_FACTOR = 0.997;
	
	public void
	strategy(CollisionMesh2Cwh mesh, double updateDeltaNanos)
	{
		
		
		switch (strategy)
		{
		case 1:
		{
			//defensiveStrategy1(mesh, updateDeltaSecs);
		}
		break;
		case 2:
		{
			
			if (defensiveStrategy1(mesh, updateDeltaNanos)) break;
			
			offensiveStrategy1(mesh, updateDeltaNanos);		
		}//case 2
		break;
		case 3:
		{
			if (defensiveStrategy1(mesh, updateDeltaNanos)) break;
			offensiveStrategy2(mesh, updateDeltaNanos);
		}
		break;
		case 4:
		{
			if (defensiveStrategy4(mesh, updateDeltaNanos)) break;
			offensiveStrategy4(mesh, updateDeltaNanos); 
		}
		default: return;
		
		}//switch
		
		//retard velocity if we are a clone ball
		if (this.splitGeneration > 0)
		{
			this.xVel *= VELOCITY_RETARD_FACTOR;
			this.yVel *= VELOCITY_RETARD_FACTOR;
		}
		
	}
}
