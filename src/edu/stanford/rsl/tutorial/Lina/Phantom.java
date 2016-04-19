package edu.stanford.rsl.tutorial.Lina;

import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;

public class Phantom extends Grid2D{

	public Phantom(int width, int height) {
		super(width, height);
		
		//this.setAtIndex(5, 200, (float) 0.7);
		this.createRectangle(50, 50, 50, 50);
		this.createCircle(200, 200, 50);
		this.createEllipse(100, 100, 50, 20);
	}
	
	public void createRectangle(int originX, int originY, int x, int y){
		for(int i=0; i < y; i++){
			for(int j=0; j < x; j++){
				this.setAtIndex(originX + j, originY + i, (float) 0.3);
			}
		}
	}
	
	public void createCircle(int centerX, int centerY, int r){
		int originX = centerX - r;
		int originY = centerY - r;
		for(int i=0; i < 2*r; i++){
			for(int j=0; j < 2*r; j++){
				if(Math.pow((originX + j - centerX), 2) + Math.pow((originY + i - centerY), 2) <= Math.pow(r, 2) ){
					this.setAtIndex(originX + j, originY + i, (float) 0.5);
				}
				
			}
		}
	}
	
	public void createEllipse(int centerX, int centerY, int a, int b){
		int originX = centerX - a;
		int originY = centerY - b;
		for(int i=0; i < 2*b; i++){
			for(int j=0; j < 2*a; j++){
				if(Math.pow((originX + j - centerX), 2) / Math.pow(a, 2) + Math.pow((originY + i - centerY), 2) / Math.pow(b, 2) <= 1 ){
					this.setAtIndex(originX + j, originY + i, (float) 0.7);
				}
				
			}
		}
	}

	public static void main(String args[]){
		new ImageJ();
		//create Phantom

		Phantom phantom = new Phantom(250,300); 
		phantom.show();
		
	}
}
