
package edu.stanford.rsl.tutorial.Lina;

import java.util.ArrayList;

import edu.stanford.rsl.conrad.data.numeric.Grid1D;
import edu.stanford.rsl.conrad.data.numeric.Grid1DComplex;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import edu.stanford.rsl.conrad.geometry.shapes.simple.Box;
import edu.stanford.rsl.conrad.geometry.shapes.simple.PointND;
import edu.stanford.rsl.conrad.geometry.shapes.simple.StraightLine;
import edu.stanford.rsl.conrad.numerics.SimpleVector;
import edu.stanford.rsl.conrad.utils.FFTUtil;
import edu.stanford.rsl.tutorial.parallel.ParallelProjector2D;
import edu.stanford.rsl.tutorial.phantoms.SheppLogan;
import ij.ImageJ;

public class ParallelBeam {

	public static Grid2D sinogram(Grid2D input, int nrProj, double detSpacing, int nrDetPixels) {
		
		if(nrProj <= 0){
			System.err.println("invalid number of Projections.");
			return new Grid2D(1, 1);
		}
		Grid2D sino = new Grid2D(nrProj, nrDetPixels);
		sino.setSpacing(detSpacing, detSpacing);
		sino.setOrigin(-(nrProj*detSpacing)/2, -(nrDetPixels*detSpacing)/2);
		
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

		//double theta = 0;
		double angle = 0;
		if (nrProj != 1) {
			angle = 180 / (nrProj - 1) *2*Math.PI / 360; // radians
		}

		// for all Projections
		for (int i = 0; i < nrProj; i++) {

			//double cosTheta = Math.cos(i * theta);
			//double sinTheta = Math.sin(i * theta);
			double cosTheta = Math.cos(i * angle);
			double sinTheta = Math.sin(i * angle);

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
				//create value in sinogram
				sino.setAtIndex(i, j, value); //nrProjections, nrDetPixels, value
				
			}
			//theta = +angle;
		}
		return sino;
	}
	
	
	public static Grid2D backProjection(Grid2D sino ){
		
		int dimension1 = sino.getWidth();
		int dimension2 = sino.getHeight();
		
		if(dimension1 < 180){
			System.err.println("The number of Projections has to be >= 180.");
			return null;
		}
		
		Grid2D image = new Grid2D(dimension2, dimension2);
		image.setSpacing(sino.getSpacing()[1], sino.getSpacing()[1]);
		image.setOrigin(-(dimension2*image.getSpacing()[0])/2, -(dimension2*image.getSpacing()[1])/2);
		
		for(int t=0; t< dimension1; t++){ 
			double theta = t* (dimension1/180) *2*Math.PI / 360;
			double cosTheta = Math.cos(theta);
			double sinTheta = Math.sin(theta);
			
			// go over pixels in image
			for(int i=0; i<image.getWidth(); i++){
				for( int j=0 ; j< image.getHeight(); j++){
					
					//pixels to world coordinates
					double[] physIndex = image.indexToPhysical(i, j);
					//calculate s and interpolate
					double s = physIndex[0]*cosTheta + physIndex[1]*sinTheta;
					double[] sinoIndex = sino.physicalToIndex(t, s);
					
					float value = InterpolationOperators.interpolateLinear(sino, t, sinoIndex[1]);
					float pixelValue = image.getAtIndex(i, j) + value;
					image.setAtIndex(i, j, pixelValue);
				}
			}
			
		}
		return image;
	}
	
	
	public static Grid2D rampFilter(Grid2D sino){
		Grid2D filteredSino = new Grid2D(sino.getSize()[0], sino.getSize()[1]);
		filteredSino.setSpacing(sino.getSpacing()[0], sino.getSpacing()[1]);
		filteredSino.setOrigin(sino.getOrigin()[0], sino.getOrigin()[1]);
		
		//ramp filter
		Grid1D ramp = new Grid1D(FFTUtil.getNextPowerOfTwo(sino.getSize()[1]));
		double deltaF = 1/(sino.getSpacing()[1]*ramp.getSize()[0]);
		for(int i=0; i < ramp.getSize()[0]/2; i++){
			ramp.setAtIndex(i, (float)(Math.abs(2.0f* Math.PI * i * deltaF )));
			ramp.setAtIndex(ramp.getSize()[0]-1-i, (float)(Math.abs(2.0f* Math.PI * i * deltaF )));
		}
		Grid1DComplex rampComplex = new Grid1DComplex(ramp);
		
		//convolution for each column
		for(int i=0; i < sino.getSize()[0]; i++){
			//Grid1DComplex complexLine = new Grid1DComplex(sino.getSubGrid(i));
			Grid1DComplex complexLine = new Grid1DComplex(sino.getSize()[1]);
			for(int j = 0; j< sino.getSize()[1]; j++){
				complexLine.setAtIndex(j, sino.getAtIndex(i, j));
			}
			complexLine.transformForward();
//			complexLine.show();
			
			//multiply with ramp filter
			for(int j = 0; j< complexLine.getSize()[0]; j++){
				complexLine.multiplyAtIndex(j, rampComplex.getRealAtIndex(j), rampComplex.getImagAtIndex(j));
			}
			
			complexLine.transformInverse();
			for(int j = 0; j< sino.getSize()[1]; j++){
				filteredSino.setAtIndex(i,j, complexLine.getRealAtIndex(j));
			}
		}
		return filteredSino;
	}
	
	
	public static Grid2D ramLakFilter(Grid2D sino){
		Grid2D filteredSino = new Grid2D(sino.getSize()[0], sino.getSize()[1]);
		filteredSino.setSpacing(sino.getSpacing()[0], sino.getSpacing()[1]);
		filteredSino.setOrigin(sino.getOrigin()[0], sino.getOrigin()[1]);
		
		//ramLak filter
		Grid1D ramLak = new Grid1D(sino.getSize()[1]); //FFTUtil.getNextPowerOfTwo(sino.getSize()[0])
		Grid1DComplex ramLakComplex = new Grid1DComplex(ramLak);

		for (int i = 0; i < ramLakComplex.getSize()[0]/2; i++) {
			if (i == 0) { // n=0
				// System.out.println(ramLak.getAtIndex(i));
				ramLakComplex.setAtIndex(i, 0.25f);
			} else if (i % 2 == 0) { // n even
				ramLakComplex.setAtIndex(i, 0);
				ramLakComplex.setAtIndex(ramLakComplex.getSize()[0] - i, 0);
			} else { // n odd
				ramLakComplex.setAtIndex(i,
						(float) (-1 / (Math.pow(Math.PI, 2) * Math.pow(i, 2))));
				ramLakComplex.setAtIndex(ramLakComplex.getSize()[0] - i,
						(float) (-1 / (Math.pow(Math.PI, 2) * Math.pow(i, 2))));
			}
		}
		
		//ramLakComplex.show("ramLakComp");
		ramLakComplex.transformForward();
		//ramLakComplex.show("ramLakComp FFT");

		// convolution for each row
		for (int i = 0; i < sino.getSize()[0]; i++) {
			//Grid1DComplex complexLine = new Grid1DComplex(sino.getSubGrid(i));
			Grid1DComplex complexLine = new Grid1DComplex(sino.getSize()[1]);
			for(int j = 0; j< sino.getSize()[1]; j++){
				complexLine.setAtIndex(j, sino.getAtIndex(i, j));
			}
			complexLine.transformForward();

			// multiply with ramp filter
			for (int j = 0; j < complexLine.getSize()[0]; j++) {
				complexLine.multiplyAtIndex(j, ramLakComplex.getRealAtIndex(j),
						ramLakComplex.getImagAtIndex(j));
			}

			complexLine.transformInverse();
			for (int j = 0; j < sino.getSize()[1]; j++) {
				filteredSino.setAtIndex(i,j, complexLine.getRealAtIndex(j));
			}
		}
		return filteredSino;
	}
	
	
	public static Grid2D filteredBackProjection(String filter, Grid2D sino){
		Grid2D fbp = new Grid2D(sino.getSize()[0], sino.getSize()[1]);
		fbp.setSpacing(sino.getSpacing()[0], sino.getSpacing()[1]);
		fbp.setOrigin(sino.getOrigin()[0], sino.getOrigin()[1]);
		
		//filtering
		switch(filter){
			case "ramp":
				System.out.println("ramp filter");
				fbp = rampFilter(sino);
				break;
			case "ramLak":
				System.out.println("ramLak filter");
				fbp = ramLakFilter(sino);
				break;
			default:
				System.err.println("this filter: ' " + filter + " ' does not exists.");
				break;
		}
		
		//backprojection
		fbp = backProjection(fbp);
		
		return fbp;
	}
	

	public static void main(String[] args) {
		
		new ImageJ();
		Phantom phan = new Phantom(200, 300, 1.0, 1.0);
		phan.show();
		
//		SheppLogan logan = new SheppLogan(256);
//		logan.show();
		
//		ParallelProjector2D proj = new ParallelProjector2D(180, 1, 400, 1);
//		Grid2D grid = proj.projectRayDriven(logan);
//		grid.show("logan");
		
		Grid2D sinogram = sinogram(phan, 180, 1.0, 400);
		sinogram.show("mein sino");
		
		Grid2D back = backProjection(sinogram);
		back.show("backprojection");
		
		Grid2D filt = rampFilter(sinogram);
		filt.show("ramp filtered sino");
		
		Grid2D filtered = ramLakFilter(sinogram);
		filtered.show("ramLak filtered sino");
		
		Grid2D fbp = filteredBackProjection("ramp", sinogram);
		fbp.show("fbp ramp");
		
		Grid2D fbpLak = filteredBackProjection("ramLak", sinogram);
		fbpLak.show("fbp ramLak");
	}
}
