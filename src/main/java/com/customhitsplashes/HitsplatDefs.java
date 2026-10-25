package com.customhitsplashes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntUnaryOperator;

final class HitsplatDefs
{
	private static final int TINT_VARBIT = 10236;

	private static final int UNTOUCHED_TYPE = 1;

	private static final int[] SPRITE = {
		2270, 3521, -1, 1361, 1633, 1632, 1629, 3405, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, 1358, 1630, 1359, 1631, 1362, 1634, 1363, 1105, 1419, 1339, 1628, 1544, 3512, 3519, 3569,
		3520, 3570, -1, -1, -1, -1, -1, 3571, 3572, 3573, 4556, 4557, -1, -1, -1, 4558, 4559, 4560, -1, -1,
		-1, 4561, 4562, 4563, -1, -1, 4564, 1360, 2245, 4763, 4764, 4765, 4766, 4767, 4768, 4769, -1, 4770, 6829, 6830,
		6831, -1,
	};

	private static final int[] CYCLES = new int[SPRITE.length];

	private static final int[] TEXT_Y = {
		0, 0, 0, 1, 2, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0,
		0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0, 2, 2, 2, 0, 0, 1, 1, 1,
		1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 0,
	};

	private static final int[][] MULTI = new int[SPRITE.length][];

	private static final Set<Integer> OTHERS = new HashSet<>();
	private static final Set<Integer> TINTED = new HashSet<>();
	private static final Set<Integer> MAX = Set.of(48, 49, 50, 51, 52, 58, 64, 70);
	private static final Set<Integer> DAMAGE = Set.of(28, 29, 48);

	static
	{
		Arrays.fill(CYCLES, 50);
		CYCLES[1] = 150;
		CYCLES[74] = 30;
		CYCLES[77] = 16;
		CYCLES[78] = 30;
		CYCLES[82] = 7;

		multi(2, 14196, 70, 68, 70);
		multi(8, TINT_VARBIT, 7, 7, 7);
		multi(9, TINT_VARBIT, 38, 7, 38);
		multi(10, TINT_VARBIT, 39, 39, 39);
		multi(11, TINT_VARBIT, 40, 39, 40);
		multi(12, TINT_VARBIT, 26, 26, 26);
		multi(13, TINT_VARBIT, 27, 26, 27);
		multi(14, TINT_VARBIT, 41, 41, 41);
		multi(15, TINT_VARBIT, 42, 41, 42);
		multi(16, TINT_VARBIT, 28, 28, 28);
		multi(17, TINT_VARBIT, 29, 28, 29);
		multi(18, TINT_VARBIT, 34, 34, 34);
		multi(19, TINT_VARBIT, 35, 34, 35);
		multi(20, TINT_VARBIT, 36, 36, 36);
		multi(21, TINT_VARBIT, 37, 36, 37);
		multi(22, TINT_VARBIT, 30, 30, 30);
		multi(23, TINT_VARBIT, 31, 30, 31);
		multi(24, TINT_VARBIT, 32, 32, 32);
		multi(25, TINT_VARBIT, 33, 32, 33);
		multi(43, 14196, 48, 28, 48);
		multi(44, 14196, 51, 34, 51);
		multi(45, 14196, 52, 36, 52);
		multi(46, 14196, 49, 30, 49);
		multi(47, 14196, 50, 32, 50);
		multi(53, TINT_VARBIT, 56, 56, 56);
		multi(54, TINT_VARBIT, 57, 56, 57);
		multi(55, 14196, 58, 56, 58);
		multi(59, TINT_VARBIT, 62, 62, 62);
		multi(60, TINT_VARBIT, 63, 62, 63);
		multi(61, 14196, 64, 62, 64);
		multi(65, TINT_VARBIT, 68, 68, 68);
		multi(66, TINT_VARBIT, 69, 68, 69);
		multi(77, 14196, 51, 34, 51);
		multi(82, TINT_VARBIT, 28, 28, 28);
	}

	private static void multi(int type, int varbit, int value0, int value1, int fallback)
	{
		MULTI[type] = new int[]{varbit, value0, value1, fallback};
		if (varbit == TINT_VARBIT && value0 != value1)
		{
			OTHERS.add(type);
			TINTED.add(value0);
		}
	}

	private HitsplatDefs()
	{
	}

	static int resolve(int type, IntUnaryOperator varbits)
	{
		if (type < 0 || type >= SPRITE.length || type == UNTOUCHED_TYPE)
		{
			return -1;
		}

		int[] multi = MULTI[type];
		if (multi == null)
		{
			return SPRITE[type] >= 0 ? type : -1;
		}

		int value = varbits.applyAsInt(multi[0]);
		int resolved = value >= 0 && value < multi.length - 2 ? multi[1 + value] : multi[multi.length - 1];
		return SPRITE[resolved] >= 0 ? resolved : -1;
	}

	static int sprite(int type)
	{
		return SPRITE[type];
	}

	static int cycles(int type)
	{
		return CYCLES[type];
	}

	static int textY(int type)
	{
		return TEXT_Y[type];
	}

	static boolean isOthers(int type)
	{
		return OTHERS.contains(type);
	}

	static boolean isMax(int type)
	{
		return MAX.contains(type);
	}

	static boolean isDamage(int type)
	{
		return DAMAGE.contains(type);
	}

	static Set<Integer> spriteIds(boolean includeTinted)
	{
		Set<Integer> ids = new HashSet<>();
		for (int type = 0; type < SPRITE.length; type++)
		{
			if (SPRITE[type] >= 0 && type != UNTOUCHED_TYPE && (includeTinted || !TINTED.contains(type)))
			{
				ids.add(SPRITE[type]);
			}
		}
		return ids;
	}
}
