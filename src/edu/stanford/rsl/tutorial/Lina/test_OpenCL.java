package edu.stanford.rsl.tutorial.Lina;
import ij.ImageJ;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.conrad.data.numeric.opencl.OpenCLGrid2D;
import edu.stanford.rsl.conrad.opencl.OpenCLUtil;


public class test_OpenCL {
	
public static Grid2D backProjectionCL(Grid2D sino, int workSize ){
		
		if(sino.getSize()[0]< 180){
			System.err.println("The number of Projections has to be >= 180.");
			return null;
		}
		
        CLContext context = OpenCLUtil.getStaticContext();
        CLDevice clDevice = context.getMaxFlopsDevice();
        CLImageFormat format = new CLImageFormat(ChannelOrder.INTENSITY, ChannelType.FLOAT);
       
        OpenCLGrid2D sinoCL = new OpenCLGrid2D(sino);
        CLImage2d<FloatBuffer> sinoTex = null;
        sinoTex = context.createImage2d(sinoCL.getDelegate().getCLBuffer().getBuffer(), sino.getSize()[0], sino.getSize()[1], format, Mem.READ_ONLY);
		
		Grid2D image = new Grid2D(sino.getSize()[1], sino.getSize()[1]);
		image.setSpacing(sino.getSpacing()[1], sino.getSpacing()[1]);
		image.setOrigin(-(sino.getSize()[1]*image.getSpacing()[0])/2, -(sino.getSize()[1]*image.getSpacing()[1])/2);
		
		float[] bufferImage = image.getBuffer().clone();
        CLBuffer<FloatBuffer> bufferCLimage = context.createFloatBuffer(bufferImage.length,Mem.READ_WRITE);
        bufferCLimage.getBuffer().put(bufferImage);
        bufferCLimage.getBuffer().rewind();
        
        CLProgram program = null;
        try{
        	program = context.createProgram(test_OpenCL.class.getResourceAsStream("backProjectionCL.cl")).build();
        }catch(IOException e){
        	e.printStackTrace();
        }
        CLKernel kernelFunktion = program.createCLKernel("backproj");
        kernelFunktion.rewind();
        
        int localWorkSize = workSize;
        int globalWorkSize = OpenCLUtil.roundUp(localWorkSize, bufferImage.length);
        
        CLCommandQueue queue = clDevice.createCommandQueue();
        
        queue.putWriteImage(sinoTex, true).putWriteBuffer(bufferCLimage, true).finish();
        
        kernelFunktion.putArg(image.getSize()[0]).putArg(image.getSize()[1])
        .putArg((float)image.getOrigin()[0]).putArg((float)image.getOrigin()[1])
        .putArg((float)image.getSpacing()[0]).putArg((float)image.getSpacing()[1])
        .putArg(sino.getSize()[0]).putArg(sino.getSize()[1])
        .putArg((float)sino.getOrigin()[0]).putArg((float)sino.getOrigin()[1])
        .putArg(sinoTex).putArg(bufferCLimage);
        
        queue.put2DRangeKernel(kernelFunktion,0, 0, globalWorkSize, globalWorkSize, localWorkSize, localWorkSize).finish();
        queue.putReadBuffer(bufferCLimage, true).finish();
		
        bufferCLimage.getBuffer().rewind();
        for (int j = 0; j < image.getSize()[1]; j++) {
        	for (int i = 0; i < image.getSize()[0]; i++) {
        		image.setAtIndex(i, j, bufferCLimage.getBuffer().get());
        	}
        }
        
        queue.release();
        kernelFunktion.release();
        program.release();
        bufferCLimage.release();
        sinoTex.release();
        sinoCL.release();
        context.release();
        
        return image;
	}
	

    public static Grid2D addGPU(int width, int height){
    	
        Phantom phantom = new Phantom(width,height,1.0,1.0);
        CLContext context = OpenCLUtil.getStaticContext();
        CLDevice clDevice = context.getMaxFlopsDevice();
       
        float[] buffer = phantom.getBuffer().clone();
        CLBuffer<FloatBuffer> bufferCL = context.createFloatBuffer(buffer.length,Mem.READ_WRITE);
        bufferCL.getBuffer().put(buffer);
        bufferCL.getBuffer().rewind();
        
        float[] buffer2 = phantom.getBuffer().clone();
        CLBuffer<FloatBuffer> bufferCL2 = context.createFloatBuffer(buffer2.length,Mem.READ_WRITE);
        bufferCL2.getBuffer().put(buffer2);
        bufferCL2.getBuffer().rewind();
        
        CLProgram program = null;
        try{
        	program = context.createProgram(test_OpenCL.class.getResourceAsStream("addOpenCL.cl")).build();
        }catch(IOException e){
        	e.printStackTrace();
        }
        CLKernel kernelFunktion = program.createCLKernel("add");
        kernelFunktion.rewind();
        
        int localWorkSize = 32;
        int globalWorkSize = OpenCLUtil.roundUp(localWorkSize, buffer.length);
        
        CLCommandQueue queue = clDevice.createCommandQueue();
        
        queue.putWriteBuffer(bufferCL, true).putWriteBuffer(bufferCL2, true).finish();
        
        kernelFunktion.putArg(width*height)
        .putArg(bufferCL).putArg(bufferCL2);
        
        queue.put1DRangeKernel(kernelFunktion, 0, globalWorkSize, localWorkSize).finish();
        
        
        queue.putReadBuffer(bufferCL, true).finish();
                     
        for (int j = 0; j < phantom.getSize()[1]; j++) {
        	for (int i = 0; i < phantom.getSize()[0]; i++) {
        		phantom.setAtIndex(i, j, bufferCL.getBuffer().get());
        	}
        }
        
        queue.release();
        kernelFunktion.release();
        program.release();
        bufferCL2.release();
        bufferCL.release();
        context.release();
        
        return phantom;
    }
 
    
    public static void main(String[] args) {
  /*      // *** Aufgabe 1 ***
        Phantom phantom = new Phantom(256,256,1.0,1.0);
        
        CLContext context = OpenCLUtil.getStaticContext();
        CLDevice clDevice = context.getMaxFlopsDevice();
        OpenCLGrid2D phantom2 = new OpenCLGrid2D(phantom, context, clDevice);
        //OpenCLGrid2D phantom2 = new OpenCLGrid2D(phantom);
        
        int num = 10;
        
        long startCPU = System.currentTimeMillis();
        for(int i = 0; i<num; i++){
            NumericPointwiseOperators.addedBy(phantom, phantom);
        }
        long endCPU = System.currentTimeMillis();
        
        long startGPU = System.currentTimeMillis();
        for(int i = 0; i<num; i++){
            NumericPointwiseOperators.addedBy(phantom2, phantom2); //addBy wasn't working!!
        }
        long endGPU = System.currentTimeMillis();
        
        System.out.println("CPU time:" + (endCPU -startCPU));
        System.out.println("GPU time:" + (endGPU -startGPU));
          
        
        // *** Aufgabe 2 ***
        Grid2D phan = addGPU(200,300);
        
        new ImageJ();
        phantom.show();
        phan.show();
        */
        
        // *** Aufgabe 3 ***
    	new ImageJ();
        Phantom p = new Phantom(256,256,1.0,1.0);
        
        ParallelBeam pb = new ParallelBeam();
        Grid2D sinogram = pb.sinogram(p, 180, 1.0, 400);
		sinogram.show("mein sino");
		
		Grid2D back = backProjectionCL(sinogram, 32);
		back.show("backprojection");
   
    }
}
