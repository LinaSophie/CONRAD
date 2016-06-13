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

	public static Grid2D fanogram(Grid2D input, double detSpacing, int nrDetElements, double incrementAngle, int nrProj, double dSI, double dSD ) {
		
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
		
		Grid2D fano = new Grid2D(nrProj, nrDetElements);
		fano.setSpacing(detSpacing, detSpacing);
		fano.setOrigin(-(fano.getSize()[0]*detSpacing)/2, -(fano.getSize()[1]*detSpacing)/2);
		
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
			double beta = (i*incrementAngle) *2*Math.PI / 360; // radians
			double sinBeta = Math.sin(beta);
			double cosBeta = Math.cos(beta);
			//System.out.println();
			//System.out.println(i*incrementAngle + " " +beta);
				
			//for all Rays
			for (int j =0; j < nrDetElements; j++) {
				
				double t = (detSpacing * j) - (detSpacing * nrDetElements) / 2;
				
				PointND p1 = new PointND( -dSI * sinBeta ,dSI * cosBeta, 0.0); //quelle
				//System.out.print(p1);
				PointND p2tmp =  new PointND( (dSD -dSI) * sinBeta , -(dSD -dSI) * cosBeta , 0.0); //senkrect auf detector
				//System.out.print(p2tmp);
				PointND p2 = new PointND( p2tmp.getCoordinates()[0] + t * cosBeta ,p2tmp.getCoordinates()[1] + t * sinBeta, 0.0);// t
				//System.out.println(p2);

				// set up line equation
				StraightLine line = new StraightLine(p1, p2);
				// compute intersections between bounding box and intersection
				// line.
				ArrayList<PointND> points = b.intersect(line);
//				if(i==0){
//					System.out.println(points);
//				}
				
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
				SimpleVector direction = line.getDirection().normalizedL2().multipliedBy(samplingStep);
				
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
					float tmp = InterpolationOperators.interpolateLinear(input, dim1 -input.getOrigin()[0], dim2 - input.getOrigin()[1]); 
					value += tmp;
					
				}
//				System.out.println(value);
				//create value in fanogram
				fano.setAtIndex(i,j, value); //nrProjections, nrDetElements, value
				
			}
		}
		return fano;
	}
	
	
	public static Grid2D rebinning(Grid2D fano, double incrementAngle, double dSI, double dSD ){
		Grid2D sino = new Grid2D(180, fano.getSize()[1]);
		sino.setSpacing(fano.getSpacing()[0], fano.getSpacing()[1]);
		sino.setOrigin(fano.getOrigin()[0], fano.getOrigin()[1]);
		
		// tan(gamma) = t/(dSI + dSD)
		// theta = beta + gamma
		// s = dSI *sin(gamma)
		
		for(int i = 0; i<sino.getSize()[0]; i++){ //theta
			for(int j = 0; j<sino.getSize()[1]; j++){ //s

				if(j >= 300){
					System.out.println();
				}
				
				double[] physIndex = sino.indexToPhysical(i, j); //theta, s

				double gamma = Math.asin(physIndex[1]/dSD); //rad!
				double t = Math.tan(gamma)*(dSD); 
				gamma = gamma *360 / (2*Math.PI); // grad
				double theta = i* sino.getSpacing()[0];//180/sino.getSize()[0];//incrementAngle; //physIndex[0]* 180/sino.getSize()[0];
				double beta = theta - gamma;
				beta = beta/incrementAngle; //?
				
// works for the case that we rotate 360°
				if(fano.getSize()[0] == 360){
					
				if(beta < 0){
					beta = beta + 360;
					if(beta > 359){
						beta = 0;
					}
				}
				}else{
				if(beta < 0){
					beta = -beta + 90 + 180 - 2* gamma;
					if(beta > fano.getSize()[0] - 1){
						beta = 0;
					}
/*					beta += 2*Math.PI;
					if(beta > 359/360*2*Math.PI){
						beta = 359/360*2*Math.PI;
					}
				}
				if(beta >= 2*Math.PI){
					beta -= 2*Math.PI;
				}
*/
				}
				}

				double[] fanoIndex = fano.physicalToIndex(beta, t);
				float val = InterpolationOperators.interpolateLinear(fano, beta, fanoIndex[1]);
				//System.out.println(" y: " + fanoIndex[1] + " val: "+ val);
				//System.out.println();
				sino.setAtIndex(i, j, val);
				
			}
		}
		return sino;
	}
	
	
	public static Grid2D shortScan(Grid2D phantom, double detSpacing, int nrDetElements, double incrementAngle, double dSI, double dSD ){
		
		double t_max = (nrDetElements/2)*detSpacing;
		double gamma = Math.atan(t_max/(dSD)) *2*Math.PI/360;
		//180 + gamma 
		double absoluteAngle = 180 + gamma;
		int nrProj = (int)(absoluteAngle / incrementAngle) + 1;
		
		Grid2D fano = fanogram(phantom, detSpacing, nrDetElements, incrementAngle, nrProj, dSI, dSD ); // (Grid2D input, double detSpacing, int nrDetElements, double incrementAngle, int nrProj, double dSI, double dSD ) {
		Grid2D sino = rebinning(fano, incrementAngle, dSI, dSD);//gute eingabe oben: 200 grad

		return sino;
	}
	
	
	public static void main(String[] args) {
		new ImageJ();
		Phantom phan = new Phantom(200, 300, 1.0, 1.0);
		phan.show();
		
	/*	Grid2D fanogram1 = fanogram(phan, 1.0, 600, 1.0, 360, 500, 1000); // (Grid2D input, double detSpacing, int nrDetElements, double incrementAngle, int nrProj, double dSI, double dSD ) {
		fanogram1.show("mein fano 360"); 

		Grid2D sino1 = rebinning(fanogram1, 1.0, 500, 1000);//gute eingabe oben: 200 grad
		sino1.show("sino 360");*/
		
		Grid2D fanogram = fanogram(phan, 1.0, 600, 1.0, 200, 500, 1000); // (Grid2D input, double detSpacing, int nrDetElements, double incrementAngle, int nrProj, double dSI, double dSD ) {
		fanogram.show("mein fano 200"); 
		//ParallelBeam p2 = new ParallelBeam();
		//Grid2D fbp2 = p2.filteredBackProjection("ramLak", fanogram);
		//fbp2.show("fbp ohne rebinning");
		
		Grid2D sino = rebinning(fanogram, 1.0, 500, 1000);//gute eingabe oben: 200 grad
		sino.show("sino 200");
		
	//	ParallelBeam p = new ParallelBeam();
		
		//Grid2D filter = p.ramLakFilter(sino);
		//filter.show("ramLak");
		
	//	Grid2D fbp = p.filteredBackProjection("ramLak", sino);
	//	fbp.show("fbp"); 
		
		/*
		Grid2D ss = shortScan(phan, 1.0, 600, 1.0, 500, 1000); // (Grid2D input, double detSpacing, int nrDetElements, double incrementAngle, int nrProj, double dSI, double dSD ) {
		ss.show("mein fano"); 
		
		ParallelBeam b = new ParallelBeam();
		
		Grid2D ssfbp = b.filteredBackProjection("ramLak", ss);
		ssfbp.show("ss fbp");*/
		
	}

}
