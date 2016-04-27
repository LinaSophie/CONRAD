
package edu.stanford.rsl.tutorial.Lina;

import java.util.ArrayList;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import edu.stanford.rsl.conrad.geometry.shapes.simple.Box;
import edu.stanford.rsl.conrad.geometry.shapes.simple.PointND;
import edu.stanford.rsl.conrad.geometry.shapes.simple.StraightLine;
import edu.stanford.rsl.conrad.numerics.SimpleVector;
import edu.stanford.rsl.tutorial.parallel.ParallelProjector2D;
import edu.stanford.rsl.tutorial.phantoms.SheppLogan;
import ij.ImageJ;

public class ParallelBeam {

	public static Grid2D sinogram(Grid2D input, int nrProj, double detSpacing, int nrDetPixels) {
		
		if(nrProj <= 0){
			System.err.println("invalid number of Projections.");
			return new Grid2D(1, 1);
		}
		Grid2D sino = new Grid2D(nrDetPixels, nrProj);
		//set sampling rate //TODO
		final double samplingStep = 3.0; //[mm]
		
		Box b = new Box((input.getSize()[0] * input.getSpacing()[0]), (input.getSize()[1] * input.getSpacing()[1]),
				0.0);
		PointND p = new PointND((double) -(input.getSize()[0] * input.getSpacing()[0]) / 2,
				(double) -(input.getSize()[1] * input.getSpacing()[1]) / 2, 0.0);
		PointND q = new PointND((double) (input.getSize()[0] * input.getSpacing()[0]) / 2,
				(double) (input.getSize()[1] * input.getSpacing()[1]) / 2, 0.0);
		b.setUpperCorner(q);
		b.setLowerCorner(p);

		double theta = 0;
		double angle = 0;
		if (nrProj != 1) {
			angle = 180 / (nrProj - 1);
		}

		// for all Projections
		for (int i = 0; i < nrProj; i++) {

			double cosTheta = Math.cos(i * theta);
			double sinTheta = Math.sin(i * theta);

			//for all Rays
			for (int j = 0; j < nrDetPixels; j++) {
				double s = (detSpacing * j) - (detSpacing * nrDetPixels) / 2;

				PointND p1 = new PointND(s * cosTheta, s * sinTheta, 0.0);
				PointND p2 = new PointND(-sinTheta + (s * cosTheta), (s * sinTheta) + cosTheta, 0.0);

				//System.out.println("p1: " + p1 + " p2: " + p2);

				// set up line equation
				StraightLine line = new StraightLine(p1, p2);
				// compute intersections between bounding box and intersection
				// line.
				ArrayList<PointND> points = b.intersect(line);

				//System.out.println(points.get(0) + " " + points.get(1));
				
				if (2 != points.size()) { 
					if (points.size() == 0) {
						line.getDirection().multiplyBy(-1.d);
						points = b.intersect(line);
					}
					if (points.size() == 0)
//						System.err.println("No points intersecing the box");
						continue;
				}

				//points which intersects the box/image
				PointND start = points.get(0); // [mm]
				PointND end = points.get(1); // [mm]
//				System.out.println("start: " + start + " end: " + end);
				
				//lenght of intersection of line
				double length = Math.sqrt(Math.pow(start.get(0) - end.get(0), 2) + Math.pow(start.get(1) - end.get(1), 2));
//				System.out.println(length);
				
				//intersection points:
				int samplingRate = (int) (length / samplingStep);
				SimpleVector direction = line.getDirection().multipliedBy(samplingStep);
				
				PointND currentPoint = new PointND(start);
				currentPoint.getAbstractVector().subtract(direction); //so that stert is the first point ;)
				
				float value= 0.0f;
				// all points for interpolation and integeration, sampling points on the line
				for(int k = 0; k<= samplingRate; k++){
					currentPoint.getAbstractVector().add(direction);;
//					System.out.println(currentPoint);
					//double[] pixels = input.physicalToIndex(currentPoint.get(0), currentPoint.get(1)); funktioniert nicht, da bild noch mal verschoben wird :(
					double x = (currentPoint.get(0)/ input.getSpacing()[0]);
					double y = (currentPoint.get(1)/ input.getSpacing()[1]);
					float tmp = InterpolationOperators.interpolateLinear(input, x+input.getOrigin()[0], y+input.getOrigin()[1]); 
					value += tmp;
					
				}
//				System.out.println(value);
				//create value in sinogram
				sino.setAtIndex(j, i, value); //nrProjections, nrDetPixels, value
				
			}

			theta = +angle;
		}

		return sino;
	}

	public static void main(String[] args) {
		
		new ImageJ();
		Phantom phan = new Phantom(200, 300, 1.0, 1.0);
		phan.show();
		
		SheppLogan logan = new SheppLogan(256);
		logan.show();
		
		ParallelProjector2D proj = new ParallelProjector2D(180, 1, 400, 1);
		Grid2D grid = proj.projectRayDriven(logan);
		grid.show("logan");
		
		Grid2D sinogram = sinogram(logan, 180, 1.0, 400);
		sinogram.show("mein sino");
		
	}

}
