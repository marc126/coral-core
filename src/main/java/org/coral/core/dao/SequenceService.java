package org.coral.core.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.coral.core.entity.CoreSequence;
import org.coral.core.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 序列发生器管理manager
 * 
 */
@Service
@Transactional(readOnly = false)
public class SequenceService extends BaseService<CoreSequence> {

	/**
	 * 一次取出的最大序号
	 */
	private final  long MAX_SEQ_CACHE_COUNT = 10;
	
	/**
	 * 序号缓存
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Long> seqCache = new HashMap();

	/**
	 * 最大号缓存
	 */
	private Map<String, Long> maxSeqCache = new HashMap();
	
	@Transactional(readOnly = false)
	public synchronized long createNext(String key) {
		return createNextSeq(key);
	}
	

	public void setMaxValue(String key,Long value){
		CoreSequence seq = getSequence(key);
		if(seq == null || seq.getMyvalue() < value){
			saveProperty(key,value);
		}
	}

	private  long createNextSeq(String key) {
		long next = getProperty(key);
		if (maxSeqCache.containsKey(key)) {
			Long maxIndex = maxSeqCache.get(key);

			Long index = seqCache.get(key);
			if (index == null)
				index = 0L;

			if (++index > maxIndex) {
				savePropertyNextMaxSeq(key);
			}

			next = index;

		} else {
			savePropertyNextMaxSeq(key);
		}

		seqCache.put(key, next);
		return next;
	}

	private  void savePropertyNextMaxSeq(String key) {
		long propValue = getProperty(key);
		propValue += MAX_SEQ_CACHE_COUNT;

		maxSeqCache.put(key, propValue);
		saveProperty(key, propValue);
	}

	private long getProperty(String key) {
		if (seqCache.containsKey(key))
			return seqCache.get(key);
		else
			return getPropertyValue(key, 0);
	}

	private long getPropertyValue(String key,long defaultValue){
		CoreSequence seq = getSequence(key);
		return (seq == null)?defaultValue:seq.getMyvalue();		
	}
	
	private CoreSequence getSequence(String key){
		List seqs = this.createQuery("from CoreSequence where mykey = :key")
		.setString("key", key)
		.list();
		return (seqs == null || seqs.isEmpty()) ? null : (CoreSequence) seqs.get(0);
	}

	private void saveProperty(String key, Long value) {
		CoreSequence seq = getSequence(key);
		if(seq == null){
			seq = new CoreSequence();
			seq.setMykey(key);
		}
			
		seq.setMyvalue(value);
		this.save(seq);
	}	
}
