JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://travis-ci.org/jacoco/jacoco.svg?branch=master)](https://travis-ci.org/jacoco/jacoco)
[![Build status](https://ci.appveyor.com/api/projects/status/g28egytv4tb898d7/branch/master?svg=true)](https://ci.appveyor.com/project/JaCoCo/jacoco/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/org.jacoco/jacoco.svg)](http://search.maven.org/#search|ga|1|g%3Aorg.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License. Check the [project homepage](http://www.jacoco.org/jacoco)
for downloads, documentation and feedback.

Please use our [mailing list](https://groups.google.com/forum/?fromgroups=#!forum/jacoco)
for questions regarding JaCoCo which are not already covered by the
[extensive documentation](http://www.jacoco.org/jacoco/trunk/doc/).

Note: We do not answer general questions in the project's issue tracker. Please use our [mailing list](https://groups.google.com/forum/?fromgroups=#!forum/jacoco) for this.
-------------------------------------------------------------------------

### **实现方式**
> 现大多代码管理居于git控制，项目迭代的差异在于git分支版本之间的代码差异。主要实现方法在于修改jacoco报告生成的源码，其中涉及git代码diff，jdt的AST获取源码。
- ClassProbesAdapter类中的 “visitMethod” 方法的作用为覆盖率计算，被测应用的每个类的方法都需调用该方法来统计代码的覆盖率。改造原理：通过Jgit.jar进行代码diff，统计改动的方法以及改动的行数。只对方法级进行统计。
```
@Override
   public final MethodVisitor visitMethod(final int access, final String name,
         final String desc, final String signature, final String[] exceptions) {
      final MethodProbesVisitor methodProbes;
      final MethodProbesVisitor mv = cv.visitMethod(access, name, desc,
            signature, exceptions);
      //jacoco源码
//    if (mv !=null) {
//       methodProbes = mv;
//    } else {
//       // We need to visit the method in any case, otherwise probe ids
//       // are not reproducible
//       methodProbes = EMPTY_METHOD_PROBES_VISITOR;
//    }
      // 改造后的代码：判断是否计算覆盖率
      if (mv !=null && isContainsMethod(name, CoverageBuilder.classInfos)) {
         methodProbes = mv;
      } else {
         // We need to visit the method in any case, otherwise probe ids
         // are not reproducible
         methodProbes = EMPTY_METHOD_PROBES_VISITOR;
      }

      return new MethodSanitizer(null, access, name, desc, signature,
            exceptions) {

         @Override
         public void visitEnd() {
            super.visitEnd();
            LabelFlowAnalyzer.markLabels(this);
            final MethodProbesAdapter probesAdapter = new MethodProbesAdapter(
                  methodProbes, ClassProbesAdapter.this);
            if (trackFrames) {
               final AnalyzerAdapter analyzer = new AnalyzerAdapter(
                     ClassProbesAdapter.this.name, access, name, desc,
                     probesAdapter);
               probesAdapter.setAnalyzer(analyzer);
               methodProbes.accept(this, analyzer);
            } else {
               methodProbes.accept(this, probesAdapter);
            }
         }
      };
   }
   
   private boolean isContainsMethod(String currentMethod, List<ClassInfo> classInfos) {
   if (classInfos== null || classInfos.isEmpty()) {
      return true;
   }
   String currentClassName = name.replaceAll("/",".");
   for (ClassInfo classInfo : classInfos) {
      String className = classInfo.getPackages() + "." + classInfo.getClassName();
      if (currentClassName.equals(className)) {
         for (MethodInfo methodInfo: classInfo.getMethodInfos()) {
            String methodName = methodInfo.getMethodName();
            if (currentMethod.equals(methodName)) {
               return true;
            }
         }
      }
   }
   return false;
}
```
- 报告的生成主要体现在源码展示，修改org.jacoco.report下的SourceHighlighter.java
```
//  修改每行代码的报告样式
private void renderCodeLine(final HTMLElement pre, final String linesrc,
      final ILine line, final int lineNr, final String classPath) throws IOException {
   if (CoverageBuilder.classInfos == null || CoverageBuilder.classInfos.isEmpty()) {
      // 全量覆盖
      highlight(pre, line, lineNr).text(linesrc);
      pre.text("\n");
   } else {
      // 增量覆盖
      boolean existFlag = true;
      for (ClassInfo classInfo : CoverageBuilder.classInfos) {
          String tClassPath = classInfo.getPackages() + "." + classInfo.getClassName();
          if (classPath.equals(tClassPath)) {
            // 新增的类
            if (classInfo.getType().equals("ADD")) {
               highlight(pre, line, lineNr).text("+ " + linesrc);
               pre.text("\n");
            } else {
               // 修改的类
               boolean flag = false;
               List<int[]> addLines = classInfo.getAddLines();
               for (int[] ints: addLines) {
                  if (ints[0] <= lineNr &&  lineNr <= ints[1]){
                     flag = true;
                     break;
                  }
               }
               if (flag) {
                  highlight(pre, line, lineNr).text("+ " + linesrc);
                  pre.text("\n");
               } else {
                  highlight(pre, line, lineNr).text(" " + linesrc);
                  pre.text("\n");
               }
            }
            existFlag = false;
            break;
          }
      }
      if (existFlag) {
         highlight(pre, line, lineNr).text(" " + linesrc);
         pre.text("\n");
      }
   }
}
```
- 实例
> git操作需要有权限，操作之前需先赋予权限：GitAdapter.setCredentialsProvider("cangzhu", "test1234");
```
private IBundleCoverage analyzeStructure() throws IOException {
        //定义本地git路径和分支名称，将会自动与master代码diff
        final CoverageBuilder coverageBuilder = new CoverageBuilder("C:\\workspace\\companywork\\demo","jacoco-cov");
//        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        return coverageBuilder.getBundle(title);
```

### 打包构建
org.jacoco.core模块依赖org.eclipse.jdt.core包的版本下属的依赖包需在jdk11编译，因此需更换成3.10.0
