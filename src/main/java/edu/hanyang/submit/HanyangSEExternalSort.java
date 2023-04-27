package edu.hanyang.submit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
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
	
	private int leaf_cmp(int l, int r) {
		if (leaf[0][l] != leaf[0][r]) return (leaf[0][l] > leaf[0][r] ? l : r);
		if (leaf[1][l] != leaf[1][r]) return (leaf[1][l] > leaf[1][r] ? l : r);
		return (leaf[2][l] > leaf[2][r] ? l : r);
	}
	
	private void leaf_swap(int l, int r) {
		int tmp[] = {leaf[0][l], leaf[1][l], leaf[2][l]};
		for (int i = 0 ; i < 3 ; i++) {
			leaf[i][l] = leaf[i][r];
			leaf[i][r] = tmp[i];
		}
		tmp = null;
	}

	private void print(int i) {
		i = heap[i];
		System.out.println(i + " : " + hval[0][i] + " " + hval[1][i] + " " + hval[2][i]);
	}
	
	private void leaf_quick_sort(int l, int r) {
		// TODO: quicksort를 구현, leaf[0] ~ leaf[2]의 값을 순차적으로 비교하도록 한다.
		
		if (l >= r) return;
		if (l+1 == r) {
			if (leaf_cmp(l, r) == l) leaf_swap(l, r);
			return;
		}
		
		int mid = (l+r)/2;
		int pivot = leaf_cmp(l, r);
		if (leaf_cmp(pivot, mid) == pivot) pivot = leaf_cmp(pivot == l ? r : l, mid);
		// pivot은 leaf[l], leaf[mid], leaf[r] 중 중간값을 선택한다.
		
		if (pivot != r) leaf_swap(pivot, r);

		int lo = l;
		for (int hi = l; hi < r ; hi++) {
			if (leaf_cmp(r, hi) == r) {
				leaf_swap(lo, hi);
				lo++;
			}
		}
		leaf_swap(lo, r);
		leaf_quick_sort(l, lo-1);
		leaf_quick_sort(lo+1, r);
	}
	

	private int[] heap = null;
	private int heap_cnt = 0;
	private int[][] hval = null; // heap_value
	private DataInputStream[] buff = null;
	
	private void hval_upd(int idx) throws IOException {
		for (int i = 0 ; i < 3 ; i++) hval[i][idx] = buff[idx].readInt();
	}
	
	private int heap_cmp(int i, int j) {
		int l = heap[i-1], r = heap[j-1];
		if (hval[0][l] != hval[0][r]) return (hval[0][l] > hval[0][r] ? i : j);
		if (hval[1][l] != hval[1][r]) return (hval[1][l] > hval[1][r] ? i : j);
		return (hval[2][l] > hval[2][r] ? i : j);
	}
	
	private void heap_swap(int i, int j) {
		int tmp = heap[i-1];
		heap[i-1] = heap[j-1];
		heap[j-1] = tmp;
	}
	
	private void heap_insert(int idx) throws IOException {
  		hval_upd(idx);
		int now = heap_cnt++;
		heap[now] = idx;
		now++;
		
		while (now > 1) {
			int nxt = now>>1;
			if (heap_cmp(nxt, now) == nxt) { // nxt > now
				heap_swap(nxt, now);
			}
			else {
				break;
			}
			now = nxt;
		}
	}
	
	private int[] heap_pop() throws IOException {
		if (heap_cnt == 0) return null;
		
		int idx = heap[0];
		int[] rtn = {hval[0][idx], hval[1][idx], hval[2][idx]};
		
		if (buff[idx].available() > 0) {
			hval_upd(idx);
		}
		else {
			buff[idx].close();
			heap[0] = heap[--heap_cnt];
		}
		
		// shiftdown!
		int now = 1;
		while (true) {
			
			int le = now*2, ri = now*2+1;
			boolean L = false, R = false;
			
			if (le <= heap_cnt && heap_cmp(now, le) == now) L = true;
			if (ri <= heap_cnt && heap_cmp(now, ri) == now) R = true;
			
			if (!L && !R) break;
			
			if (L && R) {
				int nin = heap_cmp(le, ri) == le ? ri : le;
				heap_swap(now, nin);
				now = nin;
				continue;
			}
			
			if (L) {
				heap_swap(now, le);
				now = le;
			}
			else {
				heap_swap(now, ri);
				now = ri;
			}
		}
		
		return rtn;
	}
	
    @Override
    public void sort(String infile, String outfile, String tmpdir, int blocksize, int nblocks) throws IOException {
        // TODO: your code here...
    	
    	// 1. initial phase : merge tree의 leaf node가 되도록 M block 단위로 정렬한다.
//    	System.out.println(blocksize + " * " + nblocks + " = " +  blocksize*nblocks);
    	int leaf_length = (nblocks*blocksize)/(3*Integer.BYTES); // leaf node 하나의 얼마나 많은 record가 들어가는지
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

    		DataOutputStream ostream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpdir + "/" + nxt_tmp_file + ".data"), blocksize));
    		for (int i = 0 ; i < now_size ; i++) {
    			for (int j = 0 ; j < 3 ; j++) ostream.writeInt(leaf[j][i]);
    		}
    		ostream.close();
    		
    		nxt_tmp_file++;
    		leaf = null;
    	}
    	
    	istream.close();
    	
    	// 2. external sort
    	_externalMergeSort(tmpdir, outfile, 1, blocksize, nblocks);
    }
    
    private void _externalMergeSort(String tmpdir, String outfile, int step, int blocksize, int nblocks) throws IOException {
    	nxt_tmp_file = 0;
    	File[] tmp_list = (new File(tmpdir)).listFiles();
    	
    	int tmp_length = tmp_list.length;
		int M = nblocks - 1;
		int sibling = (tmp_length-1)/M + 1;
		
		for (int i = 0 ; i < sibling ; i++) {
			int using_blocks = (i+1 == sibling ? tmp_length%M : M);
			
			heap = new int[using_blocks];
			hval = new int[3][using_blocks];
			buff = new DataInputStream[using_blocks];
	    	for (int j = 0 ; j < using_blocks ; j++) {
	    		buff[j] = new DataInputStream(new BufferedInputStream(new FileInputStream(tmpdir + (M*i + j) + ".data"), blocksize));
	    		heap_insert(j);
	    	}
	    	
	    	// heap 구성 완료, 하나씩 꺼내서 적어주면 됨.
	    	
	    	DataOutputStream ostream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpdir + "/tmp.data"), blocksize));
	    	int[] node = null;
	    	while ((node = heap_pop()) != null) {
	    		for (int j = 0 ; j < 3 ; j++) ostream.writeInt(node[j]);
	    	}
	    	
	    	for (int j = 0 ; j < using_blocks ; j++) {
	    		try {
	                Files.deleteIfExists(Paths.get(tmpdir+ "/" + (M*i + j) + ".data"));
	            }
	            catch (NoSuchFileException e) {
	            }
	    	}
	    	ostream.close();
	    	
	    	File out_file = null;
	    	if (sibling == 1) out_file = new File(outfile);
	    	else out_file = new File(tmpdir + "/" + nxt_tmp_file + ".data");
	    	out_file.createNewFile();
	    	
	    	File tmp_file = new File(tmpdir + "/tmp.data");
	    	tmp_file.renameTo(out_file);
	    	
	    	out_file = null;
	    	tmp_file = null;
	    	
	    	nxt_tmp_file++;
    	}
		
		if (sibling > 1) 
	    	_externalMergeSort(tmpdir, outfile, step+1, blocksize, nblocks);
    }
}
