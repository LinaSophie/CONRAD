package edu.stanford.rsl.tutorial.Lina;

import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.NumericGrid;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;

public class Phantom extends Grid2D{

	public Phantom( int dimension1, int dimension2, double spacing1, double spacing2) {
		super(dimension1, dimension2);
		
		//this.setAtIndex(5, 200, (float) 0.7);
//		this.createRectangle(50, 50, 50, 50);
//		this.createCircle(200, 200, 50);
//		this.createEllipse(100, 100, 50, 20, (float) 0.8);
		
		if(dimension2 <= dimension1){
			this.createEllipse((int) (dimension1/2), (int) (dimension2/2), (int) (0.4*dimension1), (int) (0.3*dimension2), (float) 0.5);
			this.createCircle((int) (3*dimension1/4), (int) (3*dimension2/4), (int) (3*dimension2/16), (float) 0.3);
			this.createRectangle((int) ((dimension1/2)-(dimension1/4)), (int) (dimension2/2),(int) (dimension1/4), (int) (dimension2/8), (float) 0.8);
		}
		else{
			this.createEllipse((int) (dimension1/2), (int) (dimension2/2), (int) (0.3*dimension1), (int) (0.4*dimension2), (float) 0.5);
			this.createCircle((int) (3*dimension1/4), (int) (dimension2/4), (int) (3*dimension1/16), (float) 0.3);
			this.createRectangle((int) (dimension1/2), (int) (dimension2/2), (int) (dimension2/8),(int) (dimension1/4), (float) 0.8);
		}
		
		this.setSpacing(spacing1, spacing2);
		this.setOrigin(-(dimension1*spacing1)/2, -(dimension2*spacing2)/2);
		
	}
	
	public void createRectangle(int origin1, int origin2, int x, int y , float value){
		for(int i=0; i < y; i++){
			for(int j=0; j < x; j++){
				float color = this.getAtIndex(origin1 +j, origin2 +i);
				if(color > (float) 0.0) color = (float) ((color + value) / 2);
				else color = value;
				this.setAtIndex(origin1 + j, origin2 + i, color);
			}
		}
	}
	
	public void createCircle(int center1, int center2, int r, float value){
		int origin1 = center1 - r;
		int origin2 = center2 - r;
		for(int i=0; i < 2*r; i++){
			for(int j=0; j < 2*r; j++){
				if(Math.pow((origin1 + j - center1), 2) + Math.pow((origin2 + i - center2), 2) <= Math.pow(r, 2) ){
					float color = this.getAtIndex(origin1 + j, origin2 +i);
					if(color > (float) 0.0) color = (float) ((color + value) / 2);
					else color = value;
					this.setAtIndex(origin1 + j, origin2 + i, color);
				}
				
			}
		}
	}
	
	public void createEllipse(int center1, int center2, int a, int b, float value){
		int origin1 = center1 - a;
		int origin2 = center2 - b;
		for(int i=0; i < 2*b; i++){
			for(int j=0; j < 2*a; j++){
				if(Math.pow((origin1 + j - center1), 2) / Math.pow(a, 2) + Math.pow((origin2 + i - center2), 2) / Math.pow(b, 2) <= 1 ){
					float color = this.getAtIndex(origin1 +j, origin2 +i);
					if(color > (float) 0.0) color = (float) ((color + value) / 2);
					else color = value;
					this.setAtIndex(origin1 + j, origin2 + i, color);
				}
				
			}
		}
	}

	public static void main(String args[]){
		new ImageJ();
		//create Phantom

		Phantom phantom = new Phantom(250,300,1.0,1.0); 
		phantom.show("phantom1");
		
		Phantom phantom2 = new Phantom(300,200, 0.5, 0.5); 
		phantom2.show("phantom2");
		
//		NumericGrid phantom3 = NumericPointwiseOperators.addedBy(phantom, phantom2);
//		phantom3.show("AddedBy");
		
		Grid2D image = new Grid2D(250, 300);
		for(int i=0; i < 50; i++){
			for(int j=0; j < 100; j++){
				image.setAtIndex(50+j,50+i, (float) 0.5);
			}
		}
		
		NumericGrid addedPhantom = NumericPointwiseOperators.addedBy(phantom, image);
		addedPhantom.show("AddedBy1");
		
		NumericGrid addedImage = NumericPointwiseOperators.addedBy(image, phantom);
		addedImage.show("AddedBy2");
	}
}
