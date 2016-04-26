package edu.stanford.rsl.tutorial.Lina;

import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.NumericGrid;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;

public class Phantom extends Grid2D{

	public Phantom(int width, int height, double spacingX, double spacingY) {
		super(width, height);
		
		//this.setAtIndex(5, 200, (float) 0.7);
//		this.createRectangle(50, 50, 50, 50);
//		this.createCircle(200, 200, 50);
//		this.createEllipse(100, 100, 50, 20, (float) 0.8);
		
		if(width >= height){
			this.createEllipse((int) (width/2), (int) (height/2), (int) (0.4*width), (int) (0.3*height), (float) 0.5);
			this.createCircle((int) (3*width/4), (int) (3*height/4), (int) (3*height/16), (float) 0.3);
			this.createRectangle((int) ((width/2)-(width/4)), (int) (height/2),(int) (width/4), (int) (height/8), (float) 0.8);
		}
		else{
			this.createEllipse((int) (width/2), (int) (height/2), (int) (0.3*width), (int) (0.4*height), (float) 0.5);
			this.createCircle((int) (3*width/4), (int) (height/4), (int) (3*width/16), (float) 0.3);
			this.createRectangle((int) (width/2), (int) (height/2), (int) (height/8),(int) (width/4), (float) 0.8);
		}
		
		this.setSpacing(spacingX, spacingY);
		this.setOrigin((width*spacingX)/2,(height*spacingY)/2);
		
	}
	
	public void createRectangle(int originX, int originY, int x, int y , float value){
		for(int i=0; i < y; i++){
			for(int j=0; j < x; j++){
				float color = this.getAtIndex(originX +j, originY +i);
				if(color > (float) 0.0) color = (float) ((color + value) / 2);
				else color = value;
				this.setAtIndex(originX + j, originY + i, color);
			}
		}
	}
	
	public void createCircle(int centerX, int centerY, int r, float value){
		int originX = centerX - r;
		int originY = centerY - r;
		for(int i=0; i < 2*r; i++){
			for(int j=0; j < 2*r; j++){
				if(Math.pow((originX + j - centerX), 2) + Math.pow((originY + i - centerY), 2) <= Math.pow(r, 2) ){
					float color = this.getAtIndex(originX + j, originY +i);
					if(color > (float) 0.0) color = (float) ((color + value) / 2);
					else color = value;
					this.setAtIndex(originX + j, originY + i, color);
				}
				
			}
		}
	}
	
	public void createEllipse(int centerX, int centerY, int a, int b, float value){
		int originX = centerX - a;
		int originY = centerY - b;
		for(int i=0; i < 2*b; i++){
			for(int j=0; j < 2*a; j++){
				if(Math.pow((originX + j - centerX), 2) / Math.pow(a, 2) + Math.pow((originY + i - centerY), 2) / Math.pow(b, 2) <= 1 ){
					float color = this.getAtIndex(originX +j, originY +i);
					if(color > (float) 0.0) color = (float) ((color + value) / 2);
					else color = value;
					this.setAtIndex(originX + j, originY + i, color);
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
