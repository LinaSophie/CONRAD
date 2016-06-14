 
__kernel void add(int size, __global float *phan, __global float *phan2){
    int iGID = get_global_id(0);
    
    if(iGID >= size) return;
    
    phan[iGID] = phan[iGID] + phan2[iGID];
}