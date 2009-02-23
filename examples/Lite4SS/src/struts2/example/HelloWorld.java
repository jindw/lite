/*
 * $Id: HelloWorld.java 471756 2006-11-06 15:01:43Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package example;

import com.opensymphony.xwork2.ActionSupport;

/**
 * <code>Set welcome message.</code>
 */
public class HelloWorld extends ActionSupport {
	private static final long serialVersionUID = 1L;
	
	private final int[] numbers = new int[] { 1, 2, 3, 4, 5 };
	private final String[] labels = new String[] { "金", "木", "水", "火", "土" };

	private int[] myNumbers = new int[] { 3, 4 };
	private String[] myLabels = new String[] { "水", "火" };

	private int number1 = 3;
	private String label1 = "水";

	private int number2 = 4;
	private String label2 = "火";

	public String execute() throws Exception {
		return SUCCESS;
	}

	public int[] getMyNumbers() {
		return myNumbers;
	}

	public void setMyNumbers(int[] myNumbers) {
		this.myNumbers = myNumbers;
	}

	public String[] getMyLabels() {
		return myLabels;
	}

	public void setMyLabels(String[] myLabels) {
		this.myLabels = myLabels;
	}

	public int getNumber1() {
		return number1;
	}

	public void setNumber1(int number1) {
		this.number1 = number1;
	}

	public String getLabel1() {
		return label1;
	}

	public void setLabel1(String label1) {
		this.label1 = label1;
	}

	public int getNumber2() {
		return number2;
	}

	public void setNumber2(int number2) {
		this.number2 = number2;
	}

	public String getLabel2() {
		return label2;
	}

	public void setLabel2(String label2) {
		this.label2 = label2;
	}

	public int[] getNumbers() {
		return numbers;
	}

	public String[] getLabels() {
		return labels;
	}
	
}
