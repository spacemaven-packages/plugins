#include <gtest/gtest.h>

TEST(TheTestSuite, TestTest) {
    ASSERT_EQ(2, 2);
}

TEST(TheTestSuite, TestFailure) {
    ASSERT_EQ(4, 2);
}

TEST(TheTestSuiteAlso, TestTestTest) {
    ASSERT_GT(4, 2);
}

int main(int argc, char **argv) {
  testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}
