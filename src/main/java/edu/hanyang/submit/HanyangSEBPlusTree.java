package edu.hanyang.submit;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.github.hyerica_bdml.indexer.BPlusTree;


public class HanyangSEBPlusTree implements BPlusTree {

    /**
     * B+ tree를 open하는 함수(파일을 열고 준비하는 단계 구현)
     * @param metafile B+ tree의 메타정보 저장(저장할거 없으면 안써도 됨)
     * @param treefile B+ tree의 메인 데이터 저장
     * @param blocksize B+ tree 작업 처리에 이용할 데이터 블록 사이즈
     * @param nblocks B+ tree 작업 처리에 이용할 데이터 블록 개수
     * @throws IOException
     */
	
	// 다음은 메타 파일에 저장해야 할 정보
	private int root_node = 0; // 루트 노드의 번호 
	private int nxt_node = 1; // 다음 노드에 할당할 번호 
	private int blocksize = 0; // 한 블록이 몇 바이트 저장할 수 있는지
	private int fanout = 0; // 노드 하나가 몇 개의 자식을 가질 수 있는지
	private int height = 0; // B+ tree 높이 (leaf node 구별용) 
	
	RandomAccessFile tree = null;
	private byte[] int_to_byte(int n) {
		return new byte[] {(byte) (n>>24), (byte) ((n>>16)&255), (byte) ((n>>8)&255), (byte) (n&255)};
	}
	
	private int byte_to_int(byte[] bytes) {
		return (bytes[0]<<24) | (bytes[1]<<16) | (bytes[2]<<8) | bytes[3];
	}
	
	private int[] byte_to_ints(byte[] bytes, int len) {
		int[] arr = new int[len/4];
		for (int i = 0 ; i < len/4 ; i++) {
			arr[i] = byte_to_int(new byte[] {bytes[i*4], bytes[i*4+1], bytes[i*4+2], bytes[i*4+3]});
		}
		return arr;
	}
	
	private int[][] divide(int[] arr) {
		int len = arr.length;
		int[] key = new int[len/2];
		int[] ptr = new int[len/2+1];
		for (int i = 0 ; i < len ; i++) {
			if (i%2 == 1)
				key[i/2] = arr[i];
			else
				ptr[i/2] = arr[i];
		}
		return new int[][] {key, ptr};
	}
	
    @Override
    public void open(String metafile, String treefile, int blocksize, int nblocks) throws IOException {
        // TODO: your code here...
    	
    	// check if metafile exists.
    	// then : fill the fields with metafile.
    	// else : make new metafile with parameters.

		RandomAccessFile meta = new RandomAccessFile(metafile, "rw");
		this.tree = new RandomAccessFile(treefile, "rw");
		
    	if (new File(metafile).exists()) { // metafile exists
    		byte[] bytes = new byte[4];
    		meta.read(bytes);
    		this.root_node = byte_to_int(bytes);
    		meta.read(bytes);
    		this.nxt_node = byte_to_int(bytes);
    		meta.read(bytes);
    		this.blocksize = byte_to_int(bytes);
    		meta.read(bytes);
    		this.fanout = byte_to_int(bytes);
    		meta.read(bytes);
    		this.height = byte_to_int(bytes);
    		meta.close();
    	}
    	else { // metafile doesn't exist
    		this.blocksize = blocksize;
    		
    		this.fanout = blocksize/4;
    		if (this.fanout % 2 == 0) 
    			this.fanout -= 1;
    		// fanout는 편의를 위해 홀수로 설정한다.
    		meta.write(int_to_byte(this.root_node));
    		meta.write(int_to_byte(this.nxt_node));
    		meta.write(int_to_byte(this.blocksize));
    		meta.write(int_to_byte(this.fanout));
    		meta.write(int_to_byte(this.height));
    		
    		meta.close();
    	}
    }

    /**
     * B+ tree에 데이터를 삽입하는 함수
     * @param key
     * @param value
     * @throws IOException
     */
    @Override
    public void insert(int key, int value) throws IOException {
        // TODO: your code here...
    }

    /**
     * B+ tree에 있는 데이터를 탐색하는 함수
     * @param key 탐색할 key
     * @return 탐색된 value 값
     * @throws IOException
     */
    @Override
    public int search(int key) throws IOException {
        // TODO: your code here...
        return _search(key, root_node, 0);
    }
    
    private int _search(int key, int idx, int depth) throws IOException, AssertionError {
    	// TODO: cache table을 이용하면, cache table에서 저장한 노드인지 먼저 검사해보고 존재할 경우 바로 불러올 수 있다. 그렇지 않은 경우에만 tree에서 읽어오기.
    	tree.seek(this.blocksize * idx);
    	byte[] bytes = new byte[this.blocksize];
    	int len = tree.read(bytes);
    	
    	int[] block = byte_to_ints(bytes, len);
    	int[][] key_ptr = divide(block); // key_ptr[0] : key | key_ptr[1] : ptr
    	
    	if (key < key_ptr[0][0]) {
    		assert depth < height; // must be not leaf node.
    		return _search(key, key_ptr[1][0], depth+1);
    	}
    	
    	// binary search
    	int lo = 0, hi = len/8;
    	while (lo < hi) {
    		int mid = (lo+hi)/2;
    		if (key < key_ptr[0][mid]) hi = mid - 1;
    		else lo = mid;
    	}
    	
    	if (depth == height) { // leaf node
    		if (key_ptr[0][lo] == key) return key_ptr[1][lo];
    		else return -1; // not found
    	}
    	else { // non-leaf node
    		return _search(key, key_ptr[1][lo+1], depth+1);
    	}
    }

    /**
     * B+ tree를 닫는 함수, 열린 파일을 닫고 저장하는 단계
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        // TODO: your code here...
    }
}
