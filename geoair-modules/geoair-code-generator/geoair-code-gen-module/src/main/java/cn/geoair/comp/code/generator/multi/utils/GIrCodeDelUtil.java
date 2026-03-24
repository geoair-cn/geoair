package cn.geoair.comp.code.generator.multi.utils;

import java.io.File;
import java.util.*;

import cn.geoair.base.util.GutilObject;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

/** 删除代码 对于生成错误生成的代码，进行一键删除 */
public class GIrCodeDelUtil {

	// 控制台输入扫描器（全局唯一，避免重复创建/关闭）
	private static final Scanner SCANNER = new Scanner(System.in);

	// 固定匹配的文件后缀列表（精准匹配，区分大小写）
	private static final Set<String> MATCH_SUFFIXES = new HashSet<>(Arrays.asList("Mapper.xml", "Mapper.java",
			"Service.java", "Event.java", "ServiceImpl.java", "Controller.java", "AddVo.java", "DetailVo.java",
			"SearchVo.java", "UpdateVo.java", "Dao.java", "Seo.java", "Dto.java", "Po.java"));

	// 主入口：循环执行删除流程，支持多次操作
	public static void doDelete() {
		// 外层循环：支持重复执行删除操作
		while (true) {
			// 执行单次删除流程
			boolean isQuit = doSingleDelete("", "");

			// 单次流程结束后，询问是否继续
			if (isQuit) {
				System.out.println("\n===== 操作结束 =====");
				break;
			}

			System.out.print("\n是否继续删除其他文件？（输入 y/Y 继续，其他任意键退出）：");
			String continueInput = SCANNER.nextLine().trim();
			if (!"y".equalsIgnoreCase(continueInput)) {
				System.out.println("退出批量删除工具");
				break;
			}
			System.out.println("\n===== 开始新一轮删除流程 =====");
		}
		// 所有操作结束后关闭扫描器
		SCANNER.close();
	}

	/**
	 * 单次删除流程（核心逻辑）
	 * @param fileNamePrefix 文件名前缀（可为空，空则控制台输入）
	 * @param sourceRootPath 根目录（可为空，空则控制台输入）
	 * @return true-退出整个工具 false-可继续新一轮操作
	 */
	public static boolean doSingleDelete(String fileNamePrefix, String sourceRootPath) {
		try {
			// 1. 输入文件名前缀（支持取消）
			fileNamePrefix = inputFileNamePrefix(fileNamePrefix);
			// 输入环节取消，直接退出单次流程
			if (fileNamePrefix == null) {
				return true;
			}

			// 2. 输入根目录（支持取消）
			sourceRootPath = inputSourceRootPath(sourceRootPath);
			// 输入环节取消，直接退出单次流程
			if (sourceRootPath == null) {
				return true;
			}

			File rootDir = FileUtil.file(sourceRootPath);

			// 校验根目录是否存在
			if (!rootDir.exists() || !rootDir.isDirectory()) {
				System.err.println("错误：指定的根目录不存在或不是文件夹 -> " + sourceRootPath);
				// 根目录错误，允许重新尝试
				return false;
			}

			// 3. 递归搜索符合条件的文件
			System.out.println("\n===== 开始搜索文件 =====");
			System.out.println("匹配规则：");
			System.out.println("1. 文件名以【" + fileNamePrefix + "】开头（忽略大小写）");
			System.out.println("2. 文件后缀属于：" + MATCH_SUFFIXES);
			List<File> matchFiles = searchFilesByRule(rootDir, fileNamePrefix);

			// 4. 处理搜索结果
			if (matchFiles.isEmpty()) {
				System.out.println("未找到符合匹配规则的文件");
				// 无匹配文件，允许重新尝试
				return false;
			}

			// 5. 打印匹配到的文件列表
			System.out.println("\n===== 共找到 " + matchFiles.size() + " 个匹配文件 =====");
			for (int i = 0; i < matchFiles.size(); i++) {
				File file = matchFiles.get(i);
				System.out.println((i + 1) + ". " + file.getAbsolutePath());
			}

			// 6. 控制台二次确认（支持取消/重新扫描）
			String confirmInput = confirmDeleteOrRescan();
			// 取消整个工具
			if ("quit".equals(confirmInput)) {
				return true;
			}
			// 重新扫描
			if ("rescan".equals(confirmInput)) {
				return false;
			}

			// 7. 执行删除操作
			if ("delete".equals(confirmInput)) {
				executeDelete(matchFiles);
			}

			// 单次删除完成，允许继续新一轮
			return false;

		}
		catch (Exception e) {
			System.err.println("操作异常：" + e.getMessage());
			// 异常时允许重新尝试
			return false;
		}
	}

	/** 输入文件名前缀（支持输入q/Q取消） */
	private static String inputFileNamePrefix(String fileNamePrefix) {
		while (true) {
			if (GutilObject.isEmpty(fileNamePrefix)) {
				System.out.println("\n请输入文件名前缀关键词（例如：AllTypes，输入 q/Q 取消操作）：");
				fileNamePrefix = SCANNER.nextLine().trim();

				// 输入q/Q，取消操作
				if ("q".equalsIgnoreCase(fileNamePrefix)) {
					System.out.println("用户取消输入文件名前缀，退出当前流程");
					return null;
				}
			}

			// 前缀不为空，返回
			if (!GutilObject.isEmpty(fileNamePrefix)) {
				return fileNamePrefix;
			}

			// 前缀为空，提示重新输入
			System.err.println("文件名前缀不能为空，请重新输入！");
			fileNamePrefix = "";
		}
	}

	/** 输入根目录（支持输入q/Q取消） */
	private static String inputSourceRootPath(String sourceRootPath) {
		while (true) {
			if (GutilObject.isEmpty(sourceRootPath)) {
				System.out.println("请输入项目根目录（输入 q/Q 取消操作，直接回车使用默认目录）：");
				sourceRootPath = SCANNER.nextLine().trim();

				// 输入q/Q，取消操作
				if ("q".equalsIgnoreCase(sourceRootPath)) {
					System.out.println("用户取消输入根目录，退出当前流程");
					return null;
				}

				// 直接回车，使用默认目录
				if (StrUtil.isBlank(sourceRootPath)) {
					sourceRootPath = System.getProperty("user.dir");
					System.out.println("未指定项目根目录，默认使用当前项目根目录：" + sourceRootPath);
				}
			}

			// 根目录不为空，返回
			if (!GutilObject.isEmpty(sourceRootPath)) {
				return sourceRootPath;
			}

			// 根目录为空，提示重新输入
			System.err.println("根目录不能为空，请重新输入！");
			sourceRootPath = "";
		}
	}

	/** 确认删除/重新扫描/退出（y删除，r重新扫描，q退出） */
	private static String confirmDeleteOrRescan() {
		while (true) {
			System.out.println("\n===== 危险操作确认 =====");
			System.out.print("请选择操作：(y/Y=删除文件, r/R=重新扫描, q/Q=退出工具)：");
			String input = SCANNER.nextLine().trim();

			if ("y".equalsIgnoreCase(input)) {
				return "delete";
			}
			else if ("r".equalsIgnoreCase(input)) {
				return "rescan";
			}
			else if ("q".equalsIgnoreCase(input)) {
				return "quit";
			}
			else {
				System.err.println("输入无效，请重新选择：y(删除)、r(重新扫描)、q(退出)");
			}
		}
	}

	/** 执行文件删除操作 */
	private static void executeDelete(List<File> matchFiles) {
		int successCount = 0;
		int failCount = 0;
		System.out.println("\n===== 开始执行删除 =====");
		for (File file : matchFiles) {
			try {
				if (FileUtil.del(file)) {
					System.out.println("删除成功：" + file.getAbsolutePath());
					successCount++;
				}
				else {
					System.err.println("删除失败：文件无法删除 -> " + file.getAbsolutePath());
					failCount++;
				}
			}
			catch (Exception e) {
				System.err.println("删除异常：" + file.getAbsolutePath() + " -> " + e.getMessage());
				failCount++;
			}
		}
		// 打印删除结果统计
		System.out.println("\n===== 删除完成 =====");
		System.out.println("成功删除：" + successCount + " 个文件");
		System.out.println("删除失败：" + failCount + " 个文件");
	}

	/** 递归搜索符合规则的文件 */
	private static List<File> searchFilesByRule(File dir, String fileNamePrefix) {
		List<File> matchFiles = new ArrayList<>();
		File[] files = dir.listFiles();
		if (files == null) {
			return matchFiles;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				matchFiles.addAll(searchFilesByRule(file, fileNamePrefix));
			}
			else {
				String fileName = file.getName();
				boolean isPrefixMatch = StrUtil.startWithIgnoreCase(fileName, fileNamePrefix);
				boolean isSuffixMatch = false;
				for (String suffix : MATCH_SUFFIXES) {
					if (StrUtil.endWith(fileName, suffix)) {
						isSuffixMatch = true;
						break;
					}
				}
				if (isPrefixMatch && isSuffixMatch) {
					matchFiles.add(file);
				}
			}
		}
		return matchFiles;
	}

	// 测试入口（可选）
	public static void main(String[] args) {
		doDelete();
	}

}
