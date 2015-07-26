package ammonite.ops

import java.nio.file.Files
import java.nio.file.attribute._

import ammonite.ops
import pprint.{PPrinter, Config, PPrint}

import scala.collection.SortedSet



sealed trait FileType
object FileType{
  case object File extends FileType
  case object Dir extends FileType
  case object SymLink extends FileType
  case object Other extends FileType
}
object PermSet{
  implicit def pprintConfig(implicit c: Config) = PPrint(
    PPrinter[PermSet]{ (p, c) =>
      Iterator(
        "rwxrwxrwx".zip(PosixFilePermission.values()).map{ case (k, v) =>
          if(p.contains(v)) k else '-'
        }.mkString)
    }
  )
}
class PermSet(s: Set[PosixFilePermission]) extends Set[PosixFilePermission]{
  def contains(elem: PosixFilePermission) = s.contains(elem)
  def +(elem: PosixFilePermission) = new PermSet(s + elem)
  def -(elem: PosixFilePermission) = new PermSet(s - elem)
  def iterator = s.iterator
}

object stat extends Op1[ops.Path, ops.stat]{
  def apply(p: ops.Path) = ops.stat.make(
    p.last,
    Files.readAttributes(java.nio.file.Paths.get(p.toString), classOf[BasicFileAttributes]),
    Files.readAttributes(java.nio.file.Paths.get(p.toString), classOf[PosixFileAttributes])
  )
  def make(name: String, attrs: BasicFileAttributes, posixAttrs: => PosixFileAttributes) = {
    import collection.JavaConversions._
    new stat(
      name,
      attrs.size(),
      attrs.lastModifiedTime(),
      () => posixAttrs.owner,
      () => new PermSet(posixAttrs.permissions.toSet),
      if (attrs.isRegularFile) FileType.File
      else if (attrs.isDirectory) FileType.Dir
      else if (attrs.isSymbolicLink) FileType.SymLink
      else if (attrs.isOther) FileType.Other
      else ???
    )
  }
  object full extends Op1[ops.Path, ops.stat.full] {
    def apply(p: ops.Path) = ops.stat.full.make(
      p.last,
      Files.readAttributes(java.nio.file.Paths.get(p.toString), classOf[BasicFileAttributes]),
      Files.readAttributes(java.nio.file.Paths.get(p.toString), classOf[PosixFileAttributes])
    )
    def  make(name: String, attrs: BasicFileAttributes, posixAttrs: => PosixFileAttributes) = {
      import collection.JavaConversions._
      new full(
        name,
        attrs.size(),
        attrs.lastModifiedTime(),
        attrs.lastAccessTime(),
        attrs.creationTime(),
        () => posixAttrs.group(),
        () => posixAttrs.owner,
        () => new PermSet(posixAttrs.permissions.toSet),
        if (attrs.isRegularFile) FileType.File
        else if (attrs.isDirectory) FileType.Dir
        else if (attrs.isSymbolicLink) FileType.SymLink
        else if (attrs.isOther) FileType.Other
        else ???
      )
    }
  }
  case class full(name: String,
                  size: Long,
                  mtime: FileTime,
                  ctime: FileTime,
                  atime: FileTime,
                  group: () => GroupPrincipal,
                  owner: () => UserPrincipal,
                  permissions: () => PermSet,
                  fileType: FileType){
    override def productPrefix = "stat.full"
    def isDir = fileType == FileType.Dir
    def isSymLink = fileType == FileType.SymLink
    def isFile = fileType == FileType.File
  }
}


case class stat(name: String,
                size: Long,
                mtime: FileTime,
                owner: () => UserPrincipal,
                permissions: () => PermSet,
                fileType: FileType){
  def isDir = fileType == FileType.Dir
  def isSymLink = fileType == FileType.SymLink
  def isFile = fileType == FileType.File
}