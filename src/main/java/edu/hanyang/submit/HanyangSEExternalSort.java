package edu.hanyang.submit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import io.github.hyerica_bdml.indexer.ExternalSort;
import org.apache.commons.lang3.tuple.MutableTriple;

public class HanyangSEExternalSort implements ExternalSort {

    /**
     * External sorting     
     * @param infile    Input file
     * @param outfile   Output file
     * @param tmpdir    Temporary directory to be used for writing intermediate runs on 
     * @param blocksize Available blocksize in the main memory of the current system
     * @param nblocks   Available block numbers in the main memory of the current system
     * @throws IOException  Exception while performing external sort
     */
	
	private int nxt_tmp_file = 0;
	private int[][] leaf = null;
	
	private void leaf_quick_sort(int l, int r) {
		// TODO: quicksort를 구현, leaf[0] ~ leaf[2]의 값을 순차적으로 비교하도록 한다.
		
	}
	
	
	
    @Override
    public void sort(String infile, String outfile, String tmpdir, int blocksize, int nblocks) throws IOException {
        // TODO: your code here...
    	
    	// 1. initial phase : merge tree의 leaf node가 되도록 M block 단위로 정렬한다.
    	System.out.println(blocksize + " * " + nblocks + " = " +  blocksize*nblocks);
    	int leaf_length = (nblocks*blocksize)/(3*Integer.BYTES); // leaf node 하나의 얼마나 많은 record가 들어가는지]
    	int leaf_size = leaf_length * (3 * Integer.BYTES); // leaf node 하나의 얼마나 많은 byte를 차지하는지 
    	
    	DataInputStream istream = new DataInputStream(new BufferedInputStream(new FileInputStream(infile), leaf_size));
    	long remain = 0;
    	while ((remain = istream.available()) > 0) {
    		int now_size = (int) (remain > leaf_size ? (leaf_length) : remain/(3 * Integer.BYTES));
    		leaf = new int[3][now_size];
    		for (int i = 0 ; i < now_size ; i++) {
    			for (int j = 0 ; j < 3 ; j++) leaf[j][i] = istream.readInt();
    		}
    		
    		leaf_quick_sort(0, now_size-1);
    		
    		DataOutputStream ostream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpdir + "/" + nxt_tmp_file + ".data")));
    		for (int i = 0 ; i < now_size ; i++) {
    			for (int j = 0 ; j < 3 ; j++) ostream.writeInt(leaf[j][i]);
    		}
    		
    		nxt_tmp_file++;
    		leaf = null;
    	}
    	
    	istream.close();
    }
}
