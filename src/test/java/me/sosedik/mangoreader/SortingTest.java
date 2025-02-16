package me.sosedik.mangoreader;

import me.sosedik.mangoreader.util.FileNameSorterUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortingTest {

	@Test
	void testSortByNumber() {
		List<String> list = List.of(
			"Chapter 000 - Prologue",
			"Chapter 001",
			"Chapter 010 - 2 test",
			"Chapter 011 - 1 test",
			"Chapter 101",
			"Chapter 200 - Finale",
			"Chapter 220 - Epilogue"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

	@Test
	void testSortNumberIncreasing() {
		List<String> list = List.of(
			"1.png",
			"2.png",
			"10.png",
			"11.png",
			"20.png",
			"21.png",
			"100.png",
			"101.png",
			"200.png"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

	@Test
	public void testSortInvalidNumbers() {
		List<String> list = List.of(
			"038912433934281869558854523115260215001138.jpg",
			"580750328988022554059934581188421124850.jpg",
			"588630392749867998626605836665327904103068931955.jpg",
			"7140653051693686246018802954895526443522293.jpg"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

	@Test
	public void testSortPrefixed() {
		List<String> list = List.of(
			"A_001.png", "A_002.png", "A_003.png", "A_010.png", "A_011.png",
			"B_001.png", "B_002.png", "B_003.png", "B_010.png", "B_011.png"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

	@Test
	public void testSortDotted() {
		List<String> list = List.of(
			"A1.01.png", "A1.02.png", "A2.01.png", "A2.02.png",
			"B1.01.png", "B1.02.png", "B2.01.png", "B2.02.png"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

	@Test
	public void testSortMixed() {
		List<String> list = List.of(
			"002_test_1_Image1.jpg",
			"002_test_1_Image2.jpg",
			"002_test_2_Image2.jpg",
			"049_Page_15.jpg",
			"100_A_Image_12.jpg",
			"101_Page_5.jpg",
			"C_001_pre.jpg",
			"D_003_pre.jpg",
			"p001_test.jpg"
		);
		List<String> copy = new ArrayList<>(list);
		copy.sort(FileNameSorterUtil.COMPARATOR);
		assertEquals(list, copy);
	}

}
