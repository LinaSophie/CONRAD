package edu.stanford.rsl.tutorial.Lina;

import java.util.ArrayList;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import edu.stanford.rsl.conrad.geometry.shapes.simple.Box;
import edu.stanford.rsl.conrad.geometry.shapes.simple.PointND;
import edu.stanford.rsl.conrad.geometry.shapes.simple.StraightLine;
import edu.stanford.rsl.conrad.numerics.SimpleVector;
import ij.ImageJ;

public class FanBeam {

	public static Grid2D fanogram(Grid2D input, double detSpacing, int nrDetElements, double rotAngle, int nrProj, double dSI, double dSD ) {
		
		if(nrProj <= 0){
			System.err.println("invalid number of Projections.");
			return null;
		}
		
		//source and detector not hitting the element:
		double diagonal = Math.sqrt(Math.pow(input.getSize()[0], 2) + Math.pow(input.getSize()[1], 2));
		if((dSI <= diagonal/2) || ((dSD - dSI) <= diagonal/2)){
			System.err.println("the source or the detector distance is not large enought.");
			return null;
		}
		
		Grid2D fano = new Grid2D(nrDetElements, nrProj);
		fano.setSpacing(detSpacing, detSpacing);
		fano.setOrigin(-(nrDetElements*detSpacing)/2, -(nrProj*detSpacing)/2);
		
		//set sampling rate 
		final double samplingStep = 0.5; //[mm]
		
		Box b = new Box((input.getSize()[0] * input.getSpacing()[0]), (input.getSize()[1] * input.getSpacing()[1]),
				0.0);
		PointND p = new PointND((double) -(input.getSize()[0] * input.getSpacing()[0]) / 2,
				(double) -(input.getSize()[1] * input.getSpacing()[1]) / 2, 0.0);
		PointND q = new PointND((double) (input.getSize()[0] * input.getSpacing()[0]) / 2,
				(double) (input.getSize()[1] * input.getSpacing()[1]) / 2, 0.0);
		b.setUpperCorner(q);
		b.setLowerCorner(p);

		
		// for all Projections
		for (int i = 0; i < nrProj; i++) {
			double beta = (i*rotAngle) *2*Math.PI / 360; // radians
			double sinBeta = Math.sin(beta);
			double cosBeta = Math.cos(beta);
				
			//for all Rays
			for (int j = 0; j < nrDetElements; j++) {
				
				double t = (detSpacing * j) - (detSpacing * nrDetElements) / 2;
				
				PointND p1 = new PointND(dSI * cosBeta, dSI * sinBeta , 0.0); //quelle
				PointND p2tmp =  new PointND(-(dSD -dSI) * cosBeta, -(dSD -dSI) * sinBeta , 0.0); //senkrect auf detector
				PointND p2 = new PointND(p2tmp.getCoordinates()[0] + t * sinBeta, p2tmp.getCoordinates()[1] - t * cosBeta, 0.0);// t

				// set up line equation
				StraightLine line = new StraightLine(p1, p2);
				// compute intersections between bounding box and intersection
				// line.
				ArrayList<PointND> points = b.intersect(line);

				System.out.println(points.get(0) + " " + points.get(1));
				
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
				currentPoint.getAbstractVector().subtract(direction); //so that start is the first point ;)
				
				float value= 0.0f;
				// all points for interpolation and integeration, sampling points on the line
				for(int k = 0; k<= samplingRate; k++){
					currentPoint.getAbstractVector().add(direction);;
//					System.out.println(currentPoint);
					//double[] pixels = input.physicalToIndex(currentPoint.get(0), currentPoint.get(1)); 
					double dim1 = (currentPoint.get(0)/ input.getSpacing()[0]);
					double dim2 = (currentPoint.get(1)/ input.getSpacing()[1]);
					float tmp = InterpolationOperators.interpolateLinear(input, dim1 - input.getOrigin()[0], dim2 - input.getOrigin()[1]); 
					value += tmp;
					
				}
//				System.out.println(value);
				//create value in fanogram
				fano.setAtIndex(j, i, value); //nrProjections, nrDetElements, value
				
			}
		}
		return fano;
	}
	
	public static void main(String[] args) {
		new ImageJ();
		Phantom phan = new Phantom(200, 300, 1.0, 1.0);
		phan.show();
		
		Grid2D fanogram = fanogram(phan, 1.0, 400, 1.0, 180, 200, 400);
		fanogram.show("mein fano");
	}

}
