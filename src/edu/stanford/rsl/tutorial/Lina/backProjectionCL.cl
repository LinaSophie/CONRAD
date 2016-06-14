
__constant sampler_t sampler = CLK_NORMALIZED_COORDS_fALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;

__kernel void backproj(int sizeImage, __read_only image2d_t sino, __global float *image{

    int imageGID = get_global_id(0);
    if(imageGID >= sizeImage) return;
    
    //phan[iGID] = phan[iGID] + phan2[iGID];
		       
/*		for(int t=0; t< dimension1; t++){ 
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
*/		       

}