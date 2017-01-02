/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.anomd.algos;

/**
 * @author ambud
 */
public class MovingAverage {

	private double average;
	private double sum;
	private double currentValue;
	private int windowSize;
	private int currentCount;

	public MovingAverage(int windowSize) {
		this.windowSize = windowSize;
	}

	public double average(double value) {
		if (currentCount < windowSize) {
			sum += value;
			average = sum / currentCount;
			currentCount++;
		} else {
			sum = sum - currentValue + value;
			average = average / windowSize;
		}
		currentValue = value;
		return average;
	}

	/**
	 * @return the average
	 */
	public double getAverage() {
		return average;
	}

	/**
	 * @param average the average to set
	 */
	public void setAverage(double average) {
		this.average = average;
	}

	/**
	 * @return the sum
	 */
	public double getSum() {
		return sum;
	}

	/**
	 * @param sum the sum to set
	 */
	public void setSum(double sum) {
		this.sum = sum;
	}

	/**
	 * @return the currentValue
	 */
	public double getCurrentValue() {
		return currentValue;
	}

	/**
	 * @param currentValue the currentValue to set
	 */
	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}

	/**
	 * @return the windowSize
	 */
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * @param windowSize the windowSize to set
	 */
	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	/**
	 * @return the currentCount
	 */
	public int getCurrentCount() {
		return currentCount;
	}

	/**
	 * @param currentCount the currentCount to set
	 */
	public void setCurrentCount(int currentCount) {
		this.currentCount = currentCount;
	}

}
