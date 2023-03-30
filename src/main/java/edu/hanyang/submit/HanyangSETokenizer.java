package edu.hanyang.submit;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import io.github.hyerica_bdml.indexer.Tokenizer;


public class HanyangSETokenizer implements Tokenizer {
	private Analyzer analyzer = null;
	private PoterStemmer s = null;
    /**
     * Tokenizer 객체를 생성하고 준비하는 단계
     */
    @Override
    public void setup() {
        // TODO: your code here...
    	analyzer = new SimpleAnalyzer();
    	s = new PoterStemmer();
    }

    /**
     * 입력 문장을 split 및 tokenizer하는 단계
     * @param str
     * @return
     */
    @Override
    public List<String> split(String str) {
        // TODO: your code here...
    	List<String> result = new ArrayList<String>();
    	try {
    		TokenStream stream = analyzer.tokenStream(null, new StringReader(str));
    		while (Stream.incrementToken()) {
    			result.add(stemString(
    					stream.getAttribute(
    							CharTermAttribute.class).toString()));
    		}
    	}
        return null;
    }

    /**
     * Tokenizer 객체를 모두 닫는 단계
     */
    @Override
    public void clean() {
        // TODO: your code here...
    }
}
