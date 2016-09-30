/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package docs.actor

//#bytebufserializer-with-manifest
import java.nio.ByteBuffer
import akka.serialization.ByteBufferSerializer
import akka.serialization.SerializerWithStringManifest

//#bytebufserializer-with-manifest

class ByteBufferSerializerDocSpec {

  //#bytebufserializer-with-manifest
  class ExampleByteBufSerializer extends SerializerWithStringManifest with ByteBufferSerializer {
    override def identifier: Int = 1337
    override def manifest(o: AnyRef): String = "naive-toStringImpl"

    // Implement this method for compatibility with `SerializerWithStringManifest`.
    override def toBinary(o: AnyRef): Array[Byte] = {
      val buf = ByteBuffer.allocate(256) // in production code, aquire this from a BufferPool

      toBinary(o, buf)
      buf.flip()
      val bytes = Array.ofDim[Byte](buf.remaining)
      buf.get(bytes)
      bytes
    }

    // Implement this method for compatibility with `SerializerWithStringManifest`.
    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
      fromBinary(ByteBuffer.wrap(bytes), manifest)

    // Actual implementation in the ByteBuffer versions of to/fromBinary: 
    override def toBinary(o: AnyRef, buf: ByteBuffer): Unit = ??? // implement actual logic here
    override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef = ??? // implement actual logic here
  }
  //#bytebufserializer-with-manifest

}
