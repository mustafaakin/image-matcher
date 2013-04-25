package in.mustafaak.imagematcher;

import android.graphics.Bitmap;

public class SceneDetectData {
	int original_key1;
	int original_key2;	
	int original_matches;
	int dist_matches;
	int homo_matches;
	long elapsed;
	int idx;
	
	Bitmap bmp;
	
	@Override
	public String toString() {
		String result = "";
		result +=   "Matched Image Index: " + idx;
		result += "\nTotal Matches: " + original_matches;
		result += "\nDistance Filtered Matches: " + dist_matches;
		result += "\nHomography Filtered Matches: " + homo_matches;
		result += "\nElapsed: (" + elapsed + " ms.)";
		return result;
	}
}
