
__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;

__kernel void backproj(int size0, int size1,
		       float origin0, float origin1, 
		       float spacing0, float spacing1,  
		       int size0sino, int size1sino ,
		       float origin0sino, float origin1sino,
		       __read_only image2d_t sino, __global float *image){

    int xGID = get_global_id(0);
    if(xGID >= size0) return;
    
    int yGID = get_global_id(1);
    if(yGID >= size1) return;
    
    
    //phan[iGID] = phan[iGID] + phan2[iGID];
    for(int t=0; t<size1sino; t++){
	float theta = t* (size1sino/180.0) *2*M_PI_F / 360.0;
	float cosTheta = cos(theta);
	float sinTheta = sin(theta);
	
	//pixels to world coordinates
	float physIndex0 = xGID * spacing0 + origin0;
	float physIndex1 = yGID * spacing1 + origin1;

	//calculate s and interpolate
	float s = physIndex0*cosTheta + physIndex1*sinTheta;
	
	//pysical to index
	//float sinoIndex0 = (t - origin0) / spacing0;
	float sinoIndex1 = (s - origin1sino) / spacing1;
	
	
	float value = read_imagef(sino, sampler, (float2)(t+0.5f,sinoIndex1+0.5f)).x;

	image[xGID*size1 + yGID] = image[xGID*size1+ yGID] + value;
    }    
}