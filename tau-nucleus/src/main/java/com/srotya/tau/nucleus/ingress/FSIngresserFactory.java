package com.srotya.tau.nucleus.ingress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.srotya.tau.nucleus.ingress.IngressManager.PullIngresser;
import com.srotya.tau.nucleus.ingress.IngressManager.IngresserFactory;

public class FSIngresserFactory implements IngresserFactory {
	
	private int ingresserParallelism;
	private String inputDirectory;
	private File[] listFiles;
	private Map<Integer, File[]> map;
	private Properties conf;

	public FSIngresserFactory() {
	}

	@Override
	public PullIngresser buildIngresser(int taskId) throws IOException {
		return new FSIngresser(map.get(taskId));
	}

	@Override
	public int getIngresserParallelism() {
		return ingresserParallelism;
	}

	@Override
	public void initialize() throws Exception {
		if(conf==null) {
			throw new Exception("Configuration is null");
		}
		inputDirectory = conf.getProperty("ingresser.fs.directory");
		System.err.println("Input directory:"+inputDirectory);
		listFiles = new File(inputDirectory).listFiles();
		System.out.println(Arrays.asList(listFiles));
		map = new HashMap<>();
		int perFile = listFiles.length/ingresserParallelism;
		for (int j = 0; j < ingresserParallelism; j++) {
			List<File> files = new ArrayList<File>();
			for(int k=0;k<perFile;k++) {
				files.add(listFiles[j*k]);
			}
			map.put(j, files.toArray(new File[1]));
		}
	}

	@Override
	public Properties getConf() {
		return conf;
	}

	@Override
	public void setConf(Properties conf) {
		this.conf = conf;
	}

	@Override
	public void setIngresserParallelism(int ingresserParallelism) {
		this.ingresserParallelism = ingresserParallelism;
	}
	
}