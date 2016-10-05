package com.srotya.tau.interceptors.grok;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

public class NormalizedReader {

	public static void main(String[] args) throws FileNotFoundException {
		File[] files = new File(args[0]).listFiles();
		Kryo kryo = new Kryo();
		for(File file:files) {
			Input fio = new Input(new FileInputStream(file));
			while(!fio.eof()) {
				Object obj = kryo.readClassAndObject(fio);
				System.out.println(obj);
			}
			fio.close();
		}
	}

}
