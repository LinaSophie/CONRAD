 
void add(int x, int y, global float *phan, global float *phan2){
    int iGID = get_global_id(0);
    int jGID = get_global_id(1);
    
    if(iGID >= x) return;
    if(jGID >= y) return;
    
    phan[iGID,jGID]+= phan2[iGID,jGID];
}