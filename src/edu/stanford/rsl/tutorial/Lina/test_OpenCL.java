package edu.stanford.rsl.tutorial.Lina;
import java.io.IOException;
import java.nio.FloatBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.conrad.data.numeric.opencl.OpenCLGrid2D;
import edu.stanford.rsl.conrad.opencl.OpenCLUtil;
import fiji.util.FloatArray;

public class test_OpenCL {

    public static Grid2D addGPU(int width, int height){
    	
        Phantom phantom = new Phantom(width,height,1.0,1.0);
        CLContext context = OpenCLUtil.getStaticContext();
        CLDevice clDevice = context.getMaxFlopsDevice();
        OpenCLGrid2D phantomCL = new OpenCLGrid2D(phantom, context, clDevice);
        float[] buffer = phantomCL.getBuffer();
        CLBuffer<FloatBuffer> bufferCL = context.createFloatBuffer(buffer.length,Mem.READ_WRITE);
        bufferCL.getBuffer().put(buffer);
        bufferCL.getBuffer().rewind();
        
        Phantom phantom2 = new Phantom(width,height,1.0,1.0);
        CLContext context2 = OpenCLUtil.getStaticContext();
        CLDevice clDevice2 = context.getMaxFlopsDevice();
        OpenCLGrid2D phantomCL2 = new OpenCLGrid2D(phantom2, context, clDevice);
        float[] buffer2 = phantomCL2.getBuffer();
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
        kernelFunktion.putArg(width).putArg(height)
        .putArg(bufferCL).putArg(bufferCL2);
        
        int localWorkSize = 128;
        int globalWorkSize = OpenCLUtil.roundUp(128, buffer.length);
        
        CLCommandQueue queue = clDevice.createCommandQueue();
        
        queue.putWriteBuffer(bufferCL, true)
        .put1DRangeKernel(kernelFunktion, 0, globalWorkSize, localWorkSize)
        .finish();
        
        bufferCL.getBuffer().get(buffer);
        
        for (int i = 0; i < phantom.getBuffer().length; ++i) {
			phantom.getBuffer()[i] = bufferCL.getBuffer().get();
        }
        
        return phantom;
    }
 
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
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
          
        Grid2D phan = addGPU(128,128);
        phan.show();
   
    }
}
