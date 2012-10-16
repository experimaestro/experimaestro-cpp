function assert_equals(expected, observed) {
	if (observed == undefined || observed != expected)
		throw new java.lang.String.format("Expected [%s] and got [%s]", expected, observed);
}