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
	private int last_node = 0; // 트리의 마지막 노드의 번호 
	private int blocksize = 0; // 한 블록이 몇 바이트 저장할 수 있는지
	private int fanout = 0; // 노드 하나가 몇 개의 자식을 가질 수 있는지 (포인터의 개수)
	private int height = 0; // B+ tree 높이 (leaf node 구별용) 
	
	// 그 외 정보 
	private RandomAccessFile tree = null;
	private int[] path = null;
	private String metafile = null;
	
	// for debug
	private int counts = 0;
	
	private byte[] int_to_byte(int n) {
		return new byte[] {(byte) (n>>24), (byte) ((n>>16)&255), (byte) ((n>>8)&255), (byte) (n&255)};
	}
	
	private byte[] ints_to_byte(int[] arr) {
		byte[] bytes = new byte[arr.length*4];
		for (int i = 0 ; i < arr.length ; i++) {
			byte[] tmp = int_to_byte(arr[i]);
			for (int j = 0 ; j < 4 ; j++)
				bytes[4*i + j] = tmp[j];
		}
		
		return bytes;
	}
	
	private int byte_to_int(byte[] bytes) {
		return (((int) (bytes[0]&255))<<24) | (((int) (bytes[1]&255))<<16) | (((int) (bytes[2]&255))<<8) | ((int) (bytes[3]&255));
	}
	
	private int[] byte_to_ints(byte[] bytes, int len) {
		int lim = len/4;
		for (int i = 0 ; i < lim ; i++) {
			if (byte_to_int(new byte[] {bytes[i*4], bytes[i*4+1], bytes[i*4+2], bytes[i*4+3]}) == 0) {
				lim = i;
				break;
			}
		}
		
		int[] arr = new int[lim];
		for (int i = 0 ; i < lim ; i++) {
			arr[i] = byte_to_int(new byte[] {bytes[i*4], bytes[i*4+1], bytes[i*4+2], bytes[i*4+3]});
		}
		return arr;
	}
	
	private int[][] divide(int[] arr, boolean is_leaf) {
		int len = arr.length;
		int[] key = new int[len/2];
		int[] ptr = new int[len/2 + (is_leaf ? 0 : 1)];
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

		this.tree = new RandomAccessFile(treefile, "rw");
		this.metafile = metafile;

//		System.out.println(new File(metafile).exists());
    	if (new File(metafile).exists()) { // metafile exists
    		RandomAccessFile meta = new RandomAccessFile(metafile, "r");
    		byte[] bytes = new byte[4];
    		meta.read(bytes);
    		this.root_node = byte_to_int(bytes);
    		meta.read(bytes);
    		this.last_node = byte_to_int(bytes);
    		meta.read(bytes);
    		this.blocksize = byte_to_int(bytes);
    		meta.read(bytes);
    		this.fanout = byte_to_int(bytes);
    		meta.read(bytes);
    		this.height = byte_to_int(bytes);
    		meta.close();
    	}
    	else { // metafile doesn't exist
    		RandomAccessFile meta = new RandomAccessFile(metafile, "rw");
    		this.blocksize = blocksize;
    		this.fanout = blocksize/Integer.BYTES;
    		this.fanout /= 2;
//    		System.out.println(this.fanout);
    		
    		meta.write(int_to_byte(this.root_node));	
    		meta.write(int_to_byte(this.last_node));	
    		meta.write(int_to_byte(this.blocksize));	
    		meta.write(int_to_byte(this.fanout));
    		meta.write(int_to_byte(this.height));
    		
    		meta.close();
    	}
    }

    private int[] readBlock(int idx) throws IOException {
    	// TODO: cache table을 이용하면, cache table에서 저장한 노드인지 먼저 검사해보고 존재할 경우 바로 불러올 수 있다. 그렇지 않은 경우에만 tree에서 읽어오기.
//    	if (this.blocksize * idx < 0) {
//    		System.out.println(" "+idx+" "+counts+" "+last_node);
//    	}
    	tree.seek((long) this.blocksize * (long) idx);
    	byte[] bytes = new byte[this.blocksize];
    	int len = tree.read(bytes);
    	
    	int[] block = byte_to_ints(bytes, len);
//    	int[][] key_ptr = divide(block); // key_ptr[0] : key | key_ptr[1] : ptr
    	
    	bytes = null;
//    	block = null;
    	
		return block;
    	
    }
    
    private void writeBlock(int idx, int[] arr) throws IOException {
    	// TODO: cache table을 이용할 경우, 실제 B+ tree에 적혀있는 값과 동기화를 시켜주는 작업을 진행해야 함.
    	tree.seek(this.blocksize * idx);
    	tree.write(ints_to_byte(arr));
    }
    
    private int[] _insert(int key, int ptr, int[][] key_ptr, boolean is_leaf) {
    	// leaf : [p k] [p k] (p k) [p k] [p k] ...
    	// else : [p k] [p k] [p] (k p) [k p] [k p] ...
//    	System.out.print("("+key_ptr[0].length+", "+key_ptr[1].length+") ");
    	
    	int insert_idx;
    	int[] new_block = new int[2*fanout - 1 + (key_ptr[0].length+1 == fanout ? 2 : 0)];
//    	for (int i = 0 ; i < new_block.length ; i++) new_block[i] = -1;
    	// insert 되는 블록의 크기는 가득 차지 않았으면 기존의 블록만큼만 할당, 가득 찼다면 추가로 2개를 더 할당.
    	
		// copy keys less than key
		for (insert_idx = 0 ; insert_idx < key_ptr[0].length ; insert_idx++) {
			if (key < key_ptr[0][insert_idx]) {
				if (!is_leaf) 
					new_block[2*insert_idx] = key_ptr[1][insert_idx];
				break;
			}
			
			new_block[2*insert_idx] = key_ptr[1][insert_idx];
			new_block[2*insert_idx+1] = key_ptr[0][insert_idx];
		}
		
		new_block[2*insert_idx + (is_leaf ? 0 : 2)] = ptr;
		new_block[2*insert_idx+1] = key;
		
		// copy keys more than key
		for (; insert_idx < key_ptr[0].length ; insert_idx++) {
			new_block[2*insert_idx+2 + (is_leaf ? 0 : 2)] = key_ptr[1][insert_idx];
			new_block[2*insert_idx+3] = key_ptr[0][insert_idx];
		}
		
		return new_block;
    }
    
    /**
     * B+ tree에 데이터를 삽입하는 함수
     * @param key
     * @param value
     * @throws IOException
     */
    @Override
    public void insert(int key, int value) throws IOException, AssertionError {
    	counts++;	
//    	if (counts % 1000 == 0) System.out.println(height);
//    	if (counts < 10) System.out.println("! "+key+" "+value);
    	// TODO: your code here...
//    	if (key == 1) System.out.println(value);
    	int val = search(key); // for update variable 'path'
    	
    	// no duplicate key
    	assert val != -1;
    	
    	int max_key = this.fanout - 1;
    	int leaf_idx = this.path[this.height];
    	int[] block = readBlock(leaf_idx);
    	int[][] leaf_key_ptr = divide(block, true);
    	   	
    	if (leaf_key_ptr[0].length + 1 > max_key) { // split!
    		int new_key = split(key, value, leaf_idx, true);
    		if (this.height > 0) internalInsert(new_key, last_node, this.height-1);
    		else newRoot(new_key, leaf_idx, last_node);
    	}
    	else { // simple insert (leaf)
    		int[] new_block = _insert(key, value, leaf_key_ptr, true);
    		writeBlock(leaf_idx, new_block);
    	}
    }
    
    private int split(int key, int ptr, int idx, boolean is_leaf) throws IOException {
    	// must retrun first key of new node
//    	System.out.println(counts+" "+height);
		last_node++;
    	int mid = fanout/2; // fanout은 max key + 1 이다.
    	int[][] key_ptr = divide(readBlock(idx), is_leaf);
    	
    	int[] inserted = _insert(key, ptr, key_ptr, is_leaf);
    	
    	int[] old_block = new int[fanout*2 - 1];
    	int[] new_block = new int[fanout*2 - 1];

//    	for (int i = 0 ; i < old_block.length ; i++) old_block[i] = -1;
//    	for (int i = 0 ; i < new_block.length ; i++) new_block[i] = -1;
    	
    	if (!is_leaf) { // internal node split
    		// 중간에 있는 원소는 부모에게 넘김. 
        	// old_block.child <= new_block.child
    		for (int i = 0 ; i < mid ; i++) {
    			old_block[2*i] = inserted[2*i];
    			old_block[2*i+1] = inserted[2*i+1];
    		}
    		old_block[2*mid] = inserted[2*mid];
    		// 0 ~ 2*mid
    		
    		for (int i = mid+1 ; i < fanout ; i++) { 
    			new_block[2*(i - (mid+1))] = inserted[2*i];
    			new_block[2*(i - (mid+1)) + 1] = inserted[2*i+1];
    		}
    		new_block[2*(fanout - (mid+1))] = inserted[2*fanout];
    		// 2*mid+2 ~ 2*fanout
    		
        	writeBlock(idx, old_block);
        	writeBlock(last_node, new_block);
        	
    		return inserted[2*mid + 1];
    	}
    	else { // simple split (leaf)
        	// old_block.child >= new_block.child
        	for (int i = 0 ; i < mid ; i++) {
        		old_block[2*i] = inserted[2*i]; // ptr
        		old_block[2*i+1] = inserted[2*i+1]; // key
        	}
        	for (int i = mid ; i < fanout ; i++) {
        		new_block[2*(i - mid)] = inserted[2*i]; // ptr
        		new_block[2*(i - mid) + 1] = inserted[2*i+1]; // key
        	}
        	
        	writeBlock(idx, old_block);
        	writeBlock(last_node, new_block);
        	
        	return new_block[1];
    	}
    }
    
    private void newRoot(int key, int left, int right) throws IOException {
//    	System.out.println(counts +" "+ last_node);
    	root_node = ++last_node;
    	height++;
    	
    	int[] block = new int[2*fanout - 1];
    	block[0] = left;
    	block[1] = key;
    	block[2] = right;
    	
    	writeBlock(root_node, block);
    }
    
    private void internalInsert(int key, int ptr, int par_dep) throws IOException {
    	int max_key = this.fanout - 1;
    	int idx = this.path[par_dep];
    	int[][] key_ptr = divide(readBlock(idx), false);
    	if (key_ptr[0].length + 1 > max_key) {
    		int new_key = split(key, ptr, idx, false);
    		if (par_dep > 0) internalInsert(new_key, last_node, par_dep-1);
    		else newRoot(new_key, idx, last_node);
    	}
    	else { // simple insert (non-leaf)
    		int[] new_block = _insert(key, ptr, key_ptr, false);
    		writeBlock(idx, new_block);
    	}
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
    	path = new int[height+1]; // insert할 때 편하게 하려고 선언함.
//    	for (int i = 0 ; i <= height ; i++) path[i] = -1;
        int val = _search(key, root_node, 0);
        if (key == 1) {
        	for (int i = 0 ; i < height+1 ; i++) {
        		System.out.print(path[i] + " ");
        	}
        	System.out.println(); 
        }
        return val;
    }
    
    private int _search(int key, int idx, int depth) throws IOException, AssertionError {
    	path[depth] = idx;

    	int[][] key_ptr = divide(readBlock(idx), depth == height); // key_ptr[0] : key | key_ptr[1] : ptr

    	if (key_ptr[0].length == 0) System.out.println(counts); // 디버그 필요. 그리고 기본값을 모두 -1로 해야함. 0이 입력으로 들어옴.
    	if (key_ptr[0].length == 0) return -1;
    	
    	if (key < key_ptr[0][0]) {
    		if (depth == height) return -1;
    		return _search(key, key_ptr[1][0], depth+1);
    	}
    	
    	// binary search
    	int lo = 0, hi = key_ptr[0].length-1;
    	while (lo+1 < hi) {
//    		System.out.println(lo + " " + hi);
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
		RandomAccessFile meta = new RandomAccessFile(this.metafile, "rw");
		
		meta.write(int_to_byte(this.root_node));	
		meta.write(int_to_byte(this.last_node));	
		meta.write(int_to_byte(this.blocksize));	
		meta.write(int_to_byte(this.fanout));
		meta.write(int_to_byte(this.height));
		
		meta.close();
    }
}
