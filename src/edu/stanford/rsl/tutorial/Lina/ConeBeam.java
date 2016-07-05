package edu.stanford.rsl.tutorial.Lina;

import java.io.IOException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import edu.stanford.rsl.conrad.data.numeric.Grid3D;
import edu.stanford.rsl.conrad.filtering.CosineWeightingTool;
import edu.stanford.rsl.conrad.filtering.ImageFilteringTool;
import edu.stanford.rsl.conrad.filtering.RampFilteringTool;
import edu.stanford.rsl.conrad.filtering.redundancy.ParkerWeightingTool;
import edu.stanford.rsl.conrad.filtering.redundancy.TrajectoryParkerWeightingTool;
import edu.stanford.rsl.conrad.io.DennerleinProjectionSource;
import edu.stanford.rsl.conrad.reconstruction.VOIBasedReconstructionFilter;
import edu.stanford.rsl.conrad.utils.Configuration;
import edu.stanford.rsl.conrad.utils.ImageUtil;

public class ConeBeam {

	public static void main(String[] args) throws IOException {
		
		String fn = "/proj/i5fpctr/data/Exercise5/8SDR_HORIZONTAL_KNEE_0002.bin";
		DennerleinProjectionSource dennerlein = new DennerleinProjectionSource();
		FileInfo info = dennerlein.getHeaderInfo(fn);
		dennerlein.initStream(fn);
		Grid3D img = new Grid3D(info.width, info.height, info.nImages);
		for (int i = 0; i < img.getSize()[2]; i++) {
			img.setSubGrid(i, dennerlein.getNextProjection());
		}
		new ImageJ();
		img.show();
		//load image
		
		//load config
		Configuration config = new Configuration();
		config = Configuration.loadConfiguration("configConeBeam.xml");
		
		//parker weights
		// for every image! (abhaengig von winkel)
		//TrajectoryParkerWeightingTool park =new TrajectoryParkerWeightingTool();
		//park.applyToolToImage(imageProcessor);
		
		ImageFilteringTool[] standardPipeline = new ImageFilteringTool[] {
				new TrajectoryParkerWeightingTool()
		};
		config.setFilterPipeline(standardPipeline);
		
		Grid3D outputImage = ImageUtil.applyFiltersInParallel(img, standardPipeline);
		
		//show image
		new ImageJ();
		outputImage.show("just parker");

	}

}
