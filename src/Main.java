import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

//import java.lang.Math.*;
import javax.imageio.ImageIO;

public class Main {
	public static void main (String[] args)
	{
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File ("test_final.jpg"));
			transformToGray(img);
			sobel(img);
			hough(img);
		} catch (IOException e) {
			System.out.println ("Nu am putut incarca imaginea");
		}		
	}
	
	public static void transformToGray (BufferedImage img)
	{
		int imgHeight, imgWidth;
		imgHeight = img.getHeight();
		imgWidth = img.getWidth();
		int i, j, p, r, g, b, a;
		for (i = 0; i < imgHeight; i++)
			for (j = 0; j < imgWidth; j++)
			{
				// bit operations to transform to gray
				p = img.getRGB(j, i);
				r = (p >> 16) & 0xff;
				b = p & 0xff;
				g = (p >> 8) & 0xff;
				a = (p >> 24) & 0xff;
				int avg = (r + b + g) / 3;
				p = (a << 24) | (avg << 16) | (avg << 8) | avg ;
				img.setRGB(j, i, p);	
			}
		// for testing purpose
		File f = new File ("output-grey.png");
		try {
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println ("Nu a mers sa cream imaginea");
		}
	}
	
	public static void sobel (BufferedImage img) {
		int[][] Gx, Gy, mirror;
		Gx = new int[3][3];
		Gy = new int[3][3];
		int imgHeight, imgWidth;
		imgHeight = img.getHeight();
		imgWidth = img.getWidth();
		mirror = new int[imgWidth][imgHeight];
		// the matrix required for Sobel
		Gx[0][0] = -1; Gx[0][1] = 0; Gx[0][2] = 1;
		Gx[1][0] = -2; Gx[1][1] = 0; Gx[1][2] = 2;
		Gx[2][0] = -1; Gx[2][1] = 0; Gx[2][2] = 1;

		Gy[0][0] = -1; Gy[0][1] = -2; Gy[0][2] = -1;
		Gy[1][0] = 0; Gy[1][1] = 0; Gy[1][2] = 0;
		Gy[2][0] = 1; Gy[2][1] = 2; Gy[2][2] = 1;
		
		int max = -1;
		
		int x, y, auxi, auxj;
		//loops to take every pixel and apply Sobel
		for(int i = 1; i < imgWidth - 1; i++)
			for(int j = 1; j < imgHeight - 1; j++) {
				x = 0;
				y = 0;
				int p = 0;
				for (auxi = 0; auxi < 3; auxi++)
					for (auxj = 0; auxj < 3; auxj++)
					{
						//sobel application
						p = img.getRGB(i - 1 + auxi, j - 1 + auxj);
						int b = p & 0xff;
						x = x + (b * Gx[auxi][auxj]);
						y = y + (b * Gy[auxi][auxj]);
					}
				int k = (int)java.lang.Math.sqrt(x * x + y * y);
				//this is to scale in case we have low differences between the modified RGBs values
				if(max < k) {
                    max = k;
                }
				mirror[i][j] = k;
			}
		
		double scale = 255.0 / max;
		//used a coppy in order to protect the pixels value during the Sobel application
		for(int i = 1; i < imgWidth - 1; i++)
			for(int j = 1; j < imgHeight - 1; j++) {
				int k = mirror[i][j];
				k = (int) ((int) k * scale);
				int p = img.getRGB(i, j );
				p = (k << 16) | (k << 8) | k ;
				img.setRGB(i, j, p);
			}
		// for testing purposes
		File f = new File ("output-sobel.png");
		try {
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println ("Nu a mers sa cream imaginea");
			
		}
	}
	
	public static void hough (BufferedImage img) {
		int imgHeight, imgWidth;
		imgHeight = img.getHeight();
		imgWidth = img.getWidth();
		int r, t, a, b;
		int maxHW;
		int minHW = imgWidth;
		// get the size of the picture
		if(imgHeight < imgWidth)
		{
			maxHW = imgWidth;
			minHW = imgHeight;
		} else
			maxHW = imgHeight;
		int[][][] vote = new int[imgWidth][imgHeight][minHW/2];
		double PI = 3.14159265359;
		
		//this is used to "vote" for different centres
		for(int i = 1; i < imgWidth - 1; i++)
			for(int j = 1; j < imgHeight - 1; j++) {
				if((img.getRGB(i, j) & 0x000000ff) != 0)
				{
					for(r = 10; r < minHW/2; r++) 
						for(t = 0; t < 360; t++){
							a = (int) (i - r * java.lang.Math.cos(t * PI / 180));
							b = (int) (j - r * java.lang.Math.sin(t * PI / 180));
							if (a > 0 && a < imgWidth && b > 0 && b < imgHeight)
								vote[a][b][r]++;
						}
				}
			}

		setMax(vote, imgHeight, imgWidth, img, maxHW, minHW);
		
	}
	// use this to find the ideal candidates
	public static void setMax (int[][][] vote, int width, int height, BufferedImage img, int maxHW, int minHW)
	{
		ArrayList<Candidate> candidates = new ArrayList<Candidate>();
		double PI = 3.14159265359;
		int i, j, k, maxim = -1, soli= 0, solj = 0, solr = 0;
		//k is the radius, we look for candidates for different radiuses, chances to intersect are low
		for (k = 10; k < minHW/2; k++)
		{
			maxim = -1;
			for (i = 0; i < width; i++)
				for (j = 0; j < height; j++)
				{
					if (vote[j][i][k] > maxim)
					{
						maxim = vote[j][i][k];
						soli = i;
						solj = j;
						solr = k;
						
					}
				}
			candidates.add(new Candidate(soli, solj, k, vote[solj][soli][k]));
		}
		optimizationByZone(candidates);
		// we select the adequate candidates for the centers and draw the circle around them using the same formula from hough
		for (i = 0; i < candidates.size(); i++)
		{
			if ((int)3.4 * PI * candidates.get(i).r < candidates.get(i).votes && candidates.get(i).q == true)
			{
				int r = 255;
				int g=0, b=0;
				int rgb = ((r&0x0ff)<<16)|((g&0x0ff)<<8)|(b&0x0ff);
				System.out.println(candidates.get(i).x + " " + candidates.get(i).y + " " + candidates.get(i).votes + " " + candidates.get(i).r);
				img.setRGB(candidates.get(i).y, candidates.get(i).x, rgb);
				int x,y;
				for(int t = 0; t < 360; t++){
					x = (int) (candidates.get(i).x - candidates.get(i).r * java.lang.Math.cos(t * PI / 180));
					y = (int) (candidates.get(i).y - candidates.get(i).r * java.lang.Math.sin(t * PI / 180));
					if (x > 0 && x < width && y > 0 && y < height)
						img.setRGB(y, x, rgb);
				}
			}
		}
		
		File f = new File ("output-circle.png");
		try {
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println ("Nu a mers sa cream imaginea");
		}
	}
	// we tried to delete some candidates because there were multiple centers for one circle
	public static void optimizationByZone (ArrayList<Candidate> candidates)
	{
		for(int i = 0; i < candidates.size(); i++)
			for(int j = i+1; j < candidates.size(); j++) {
				if(candidates.get(i).x != candidates.get(j).x && candidates.get(i).q == true)
				if((candidates.get(i).x >= candidates.get(j).x - 20 &&
					candidates.get(i).x <= candidates.get(j).x + 20) && 
					(candidates.get(i).y >= candidates.get(j).y - 20 &&
					candidates.get(i).y <= candidates.get(j).y + 20) &&
					(candidates.get(i).r >= candidates.get(j).r - 20 &&
					candidates.get(i).r <= candidates.get(j).r + 20)) {
					
					{
						candidates.get(j).q = false;
						if (candidates.get(i).votes < candidates.get(j).votes)
							candidates.get(i).votes = candidates.get(j).votes;
					}
					if(candidates.get(j).x == 153)
					System.out.println(candidates.get(i).x + ">" + candidates.get(j).x); 
					}
			}
	}
}
