package com.laboschqpa.filehost.util;

import com.laboschqpa.filehost.entity.ImageVariant;
import lombok.AllArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class ImageVariantSelectorTest {
    private static final ImageVariant variant_100 = createImageVariantWithSize(100);
    private static final ImageVariant variant_200 = createImageVariantWithSize(200);
    private static final ImageVariant variant_400 = createImageVariantWithSize(400);
    private static final ImageVariant variant_1000 = createImageVariantWithSize(1000);
    private static final ImageVariant variant_2000 = createImageVariantWithSize(2000);

    private static final List<ImageVariant> allVariants = List.of(
            variant_100,
            variant_200,
            variant_400,
            variant_1000,
            variant_2000
    );

    /**
     * These values rely on {@link ImageVariantSelector#MAX_RELATIVE_SIZE_POSITION_TO_SERVE_THE_SMALLER_SIZED_VARIANT}!
     */
    private static List<TestParam> provideArgumentsFor_selectImageVariantToServe() {
        return List.of(
                TestParam.of(-1000, variant_100),
                TestParam.of(0, variant_100),

                TestParam.of(1, variant_100),
                TestParam.of(50, variant_100),
                TestParam.of(99, variant_100),
                TestParam.of(100, variant_100),
                TestParam.of(101, variant_100),
                TestParam.of(130, variant_100),

                TestParam.of(140, variant_200),
                TestParam.of(200, variant_200),
                TestParam.of(260, variant_200),

                TestParam.of(280, variant_400),

                TestParam.of(1000, variant_1000),
                TestParam.of(1300, variant_1000),

                TestParam.of(1400, variant_2000),
                TestParam.of(2000, variant_2000),

                TestParam.of(9999999, variant_2000)
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsFor_selectImageVariantToServe")
    void selectImageVariantToServe(TestParam testParam) {
        ImageVariant resultVariant = ImageVariantSelector.selectImageVariantToServe(allVariants, testParam.wantedSize);

        assertSame(testParam.expectedVariant, resultVariant,
                "expected: " + testParam.expectedVariant.getVariantSize() +
                        ", actual: " + resultVariant.getVariantSize());
    }

    @AllArgsConstructor
    private static class TestParam {
        private int wantedSize;
        private ImageVariant expectedVariant;

        static TestParam of(int wantedSize, ImageVariant expectedVariant) {
            return new TestParam(wantedSize, expectedVariant);
        }
    }

    private static ImageVariant createImageVariantWithSize(Integer size) {
        final ImageVariant imageVariant = new ImageVariant();
        imageVariant.setVariantSize(size);
        return imageVariant;
    }
}