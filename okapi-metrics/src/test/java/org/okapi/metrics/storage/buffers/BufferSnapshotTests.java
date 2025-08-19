package org.okapi.metrics.storage.buffers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.okapi.testutils.OkapiTestUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BufferSnapshotTests {
    @ParameterizedTest
    @MethodSource("fuzzArgs")
    public void testWriteInBlocks(int capacity, int blockSize) throws IOException {
        var alloc = new HeapBufferAllocator();
        var buffer = alloc.allocate(capacity);
        var testCase = OkapiTestUtils.genBytes(capacity);
        for(int i = 0; i < capacity; i++){
            buffer.put(testCase[i]);
        }
        var tempFile = Files.createTempFile("temp", ".tmp");
        try (var fos = new FileOutputStream(tempFile.toFile())){
            BufferSnapshot.writeInBlocks(fos, buffer, buffer.pos(), blockSize);
        }
        
        var restored = Files.readAllBytes(tempFile);
        for(int i = 0; i < capacity; i++){
            assertEquals(testCase[i], restored[i], i + "-th byte doesn't match");
        }
    }
    
    public static Stream<Arguments> fuzzArgs(){
        return Stream.of(
                Arguments.of(20, 10),
                Arguments.of(20, 1),
                Arguments.of(10, 3),
                Arguments.of(10, 10),
                Arguments.of(10, 20)
        );
    }
    
}
