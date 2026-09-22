// =====================================================================
// 星尘地牢 - 一键启动器（Windows 真 exe，无控制台窗口）
// ---------------------------------------------------------------------
// 职责：双击本程序后自动完成：
//   1. 检测可用的 Java 17 运行环境（项目自带 -> JAVA_HOME -> PATH -> 常见安装目录），
//      未安装时弹出引导窗口，由用户自行下载安装（本程序不代下载 JDK）；
//   2. 下载 JavaFX 依赖（约 9MB）到项目 .libs 目录；
//   3. 编译随包的最新源码到 out 目录（源码没变化时自动跳过）；
//   4. 用 javaw 启动游戏，本程序随即退出。
//
// 维护同步点：JavafxVersion 必须与 pom.xml 中的 javafx.version 一致。
// 重建方式：运行 launcher\build-launcher.bat（使用 Windows 自带的 csc）。
// =====================================================================
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

[assembly: System.Reflection.AssemblyTitle("星尘地牢")]
[assembly: System.Reflection.AssemblyProduct("星尘地牢")]

namespace StardustLauncher
{
    internal static class Program
    {
        // ======== 维护同步点（与 pom.xml 保持一致）========
        private const string JavafxVersion = "17.0.10";
        private const string MainClassName = "com.example.smallgame.GameApplication";
        private const int MinJavaMajorVersion = 17;
        // =================================================

        private static readonly string[] JavafxJarNames = new string[]
        {
            "javafx-base", "javafx-graphics", "javafx-controls"
        };

        private static readonly string[] MavenMirrors = new string[]
        {
            "https://maven.aliyun.com/repository/central",
            "https://repo1.maven.org/maven2"
        };

        private static string _root;
        private static string _runtimeDir;
        private static string _runtimeJdkDir;
        private static string _libsDir;
        private static string _outDir;
        private static string _classesDir;
        private static string _stampFile;
        private static string _logFile;

        [STAThread]
        private static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072; // 显式启用 TLS 1.2

            try
            {
                InitPaths();
                EnsureProjectLayout();
                bool dataDirFallback = EnsureWritableLayout();

                try
                {
                    Directory.CreateDirectory(_outDir);
                    string head = "== 星尘地牢启动 " + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " ==";
                    if (dataDirFallback) head += "\r\n提示：游戏文件夹不可写，运行时文件已重定向到 " + _libsDir + " 与 " + _outDir;
                    File.WriteAllText(_logFile, head + "\r\n", new UTF8Encoding(true));
                }
                catch { }

                string javaBin = EnsureJavaRuntime();
                EnsureJavafxLibraries();
                CompileIfNeeded(javaBin);
                LaunchGame(javaBin);
            }
            catch (OperationCanceledException)
            {
                // 用户主动取消，安静退出
            }
            catch (Exception ex)
            {
                ShowError("启动失败", ex.Message + "\n\n" + ex.ToString());
            }
        }

        // =============================================================
        // 路径与项目结构
        // =============================================================

        private static void InitPaths()
        {
            _root = AppDomain.CurrentDomain.BaseDirectory.TrimEnd('\\', '/') + "\\";
            _runtimeDir = Path.Combine(_root, ".runtime");
            _runtimeJdkDir = Path.Combine(_runtimeDir, "jdk");
            SetDataRoot(_root);
        }

        // 设置运行时产物（.libs、out、日志）的存放位置
        private static void SetDataRoot(string dataRoot)
        {
            _libsDir = Path.Combine(dataRoot, ".libs");
            _outDir = Path.Combine(dataRoot, "out");
            _classesDir = Path.Combine(_outDir, "classes");
            _stampFile = Path.Combine(_outDir, ".build-stamp");
            _logFile = Path.Combine(_outDir, "launch-log.txt");
        }

        // 运行时产物默认写在 exe 所在目录；当该目录不可写时（只读文件夹、
        // 系统保护目录、安全软件拦截等），回退到当前用户的本地应用数据目录。
        // 返回 true 表示发生了回退。
        private static bool EnsureWritableLayout()
        {
            if (IsDirWritable(_root)) return false;

            string fallback = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "StardustDungeon");
            SetDataRoot(fallback);
            if (!IsDirWritable(fallback))
            {
                throw new Exception("无法创建游戏运行所需的文件夹。\n\n已尝试：\n  1. 游戏文件夹：" + _root + "\n  2. 用户数据目录：" + fallback + "\n\n可能原因：安全软件拦截、文件夹权限不足。\n请把游戏文件夹移动到没有权限限制的位置（如桌面）后重试。");
            }
            return true;
        }

        // 尝试在指定目录下创建 .libs 与 out 并写入测试文件，探测目录是否可写
        private static bool IsDirWritable(string dir)
        {
            try
            {
                string libsProbe = Path.Combine(dir, ".libs");
                Directory.CreateDirectory(libsProbe);
                string probeFile = Path.Combine(libsProbe, ".write-test");
                File.WriteAllText(probeFile, "ok");
                File.Delete(probeFile);

                Directory.CreateDirectory(Path.Combine(dir, "out"));
                return true;
            }
            catch
            {
                return false;
            }
        }

        private static void EnsureProjectLayout()
        {
            string srcJavaDir = Path.Combine(_root, "src", "main", "java");
            string pomFile = Path.Combine(_root, "pom.xml");
            if (!Directory.Exists(srcJavaDir) || !File.Exists(pomFile))
            {
                throw new Exception("没有在本程序所在文件夹中看到游戏源码。\n\n请把「星尘地牢.exe」放回项目根目录（与 pom.xml、src 文件夹同级）后再双击运行。\n\n当前目录：" + _root);
            }
        }

        // =============================================================
        // 第 1 步：确保有可用的 Java 17（缺失时弹窗引导用户自行下载安装）
        // =============================================================

        private static string EnsureJavaRuntime()
        {
            while (true)
            {
                string bin = FindJavaBin();
                if (bin != null) return bin;

                using (JavaRequiredForm form = new JavaRequiredForm())
                {
                    Application.Run(form);
                    if (!form.RetryRequested) throw new OperationCanceledException();
                }
            }
        }

        // 依次尝试：项目内置 .runtime\jdk -> JAVA_HOME -> PATH -> 常见 JDK 安装目录。
        // JAVA_HOME / PATH 同时读取环境变量与注册表：用户刚装完 JDK 时，
        // 本进程的环境变量还是旧的，读注册表就能立刻找到新安装的 JDK。
        private static string FindJavaBin()
        {
            string bin = JavaBinIfUsable(Path.Combine(_runtimeJdkDir, "bin"));
            if (bin != null) return bin;

            // 仅供自动化测试模拟“机器上没有任何 Java”：只认项目内置运行时（玩家无需关心）
            if (Environment.GetEnvironmentVariable("STARDUST_TEST_NO_MACHINE_JAVA") == "1") return null;

            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (string.IsNullOrEmpty(javaHome)) javaHome = GetRegistryEnvironment("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                bin = JavaBinIfUsable(Path.Combine(javaHome.Trim().Trim('"'), "bin"));
                if (bin != null) return bin;
            }

            bin = FindJavaBinOnPath();
            if (bin != null) return bin;

            bin = FindJavaBinOnPathValue(GetRegistryEnvironment("Path"));
            if (bin != null) return bin;

            return FindJavaBinInDefaultLocations();
        }

        // 返回可用的 Java bin 目录（能同时找到 javac、java 且版本达标）
        private static string JavaBinIfUsable(string binDir)
        {
            if (string.IsNullOrEmpty(binDir)) return null;
            string javacPath = Path.Combine(binDir, "javac.exe");
            string javaPath = Path.Combine(binDir, "java.exe");
            if (!File.Exists(javacPath) || !File.Exists(javaPath)) return null;
            if (GetJavacMajorVersion(javacPath) < MinJavaMajorVersion) return null;
            return binDir;
        }

        private static int GetJavacMajorVersion(string javacPath)
        {
            try
            {
                int exitCode;
                string stdout;
                string stderr;
                RunCapture(javacPath, "-version", 30000, out exitCode, out stdout, out stderr);
                Match match = Regex.Match(stdout + "\n" + stderr, @"javac\s+(\d+)");
                if (match.Success) return int.Parse(match.Groups[1].Value);
            }
            catch { }
            return 0;
        }

        private static string FindJavaBinOnPath()
        {
            return FindJavaBinOnPathValue(Environment.GetEnvironmentVariable("PATH"));
        }

        private static string FindJavaBinOnPathValue(string pathVar)
        {
            if (string.IsNullOrEmpty(pathVar)) return null;
            string[] parts = pathVar.Split(';');
            for (int i = 0; i < parts.Length; i++)
            {
                string raw = parts[i].Trim().Trim('"');
                if (raw.Length == 0) continue;
                string dir;
                try { dir = Environment.ExpandEnvironmentVariables(raw).TrimEnd('\\'); }
                catch { continue; }
                if (dir.Length == 0) continue;
                string bin = JavaBinIfUsable(dir);
                if (bin != null) return bin;
            }
            return null;
        }

        // 读取系统/用户环境变量（供“重试”时获取用户刚安装 JDK 后的新值）
        private static string GetRegistryEnvironment(string name)
        {
            try
            {
                using (RegistryKey key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"))
                {
                    if (key != null)
                    {
                        string value = key.GetValue(name) as string;
                        if (!string.IsNullOrEmpty(value)) return value;
                    }
                }
            }
            catch { }

            try
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey("Environment"))
                {
                    if (key != null)
                    {
                        string value = key.GetValue(name) as string;
                        if (!string.IsNullOrEmpty(value)) return value;
                    }
                }
            }
            catch { }

            return null;
        }

        // 扫描常见 JDK 安装目录（安装包默认位置），覆盖“已安装但未配置 PATH/JAVA_HOME”的情况
        private static string FindJavaBinInDefaultLocations()
        {
            string programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            string programFilesX86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
            string[] roots = new string[]
            {
                Path.Combine(programFiles, "Eclipse Adoptium"),
                Path.Combine(programFiles, "Java"),
                Path.Combine(programFiles, "Microsoft"),
                Path.Combine(programFiles, "Amazon Corretto"),
                Path.Combine(programFilesX86, "Java")
            };

            for (int i = 0; i < roots.Length; i++)
            {
                if (!Directory.Exists(roots[i])) continue;
                string[] subDirs;
                try { subDirs = Directory.GetDirectories(roots[i]); }
                catch { continue; }
                for (int j = 0; j < subDirs.Length; j++)
                {
                    string bin = JavaBinIfUsable(Path.Combine(subDirs[j], "bin"));
                    if (bin != null) return bin;
                }
            }
            return null;
        }

        // =============================================================
        // 第 2 步：确保 JavaFX 依赖 jar（缺失/损坏时联网下载）
        // =============================================================

        private static void EnsureJavafxLibraries()
        {
            Directory.CreateDirectory(_libsDir);

            List<string> missing = new List<string>();
            for (int i = 0; i < JavafxJarNames.Length; i++)
            {
                string jarName = JavafxJarNames[i] + "-" + JavafxVersion + "-win.jar";
                string fullPath = Path.Combine(_libsDir, jarName);
                if (!File.Exists(fullPath) || !IsZipReadable(fullPath)) missing.Add(JavafxJarNames[i]);
            }
            if (missing.Count == 0) return;

            RunWithProgress("星尘地牢 - 正在下载游戏依赖", true, delegate(ProgressForm form)
            {
                for (int index = 0; index < missing.Count; index++)
                {
                    if (form.CancelRequested) throw new OperationCanceledException();

                    string baseName = missing[index];
                    string jarName = baseName + "-" + JavafxVersion + "-win.jar";
                    string destPath = Path.Combine(_libsDir, jarName);
                    string prefix = string.Format("正在下载游戏依赖（{0}/{1}）", index + 1, missing.Count);

                    bool ok = false;
                    Exception lastError = null;
                    for (int m = 0; m < MavenMirrors.Length && !ok; m++)
                    {
                        string url = MavenMirrors[m] + "/org/openjfx/" + baseName + "/" + JavafxVersion + "/" + jarName;
                        try
                        {
                            DownloadFile(form, url, destPath, prefix, index, missing.Count);
                            if (!IsZipReadable(destPath))
                            {
                                DeleteFileQuiet(destPath);
                                throw new Exception("下载的依赖文件校验失败");
                            }
                            ok = true;
                        }
                        catch (OperationCanceledException) { throw; }
                        catch (Exception ex) { lastError = ex; }
                    }

                    if (!ok)
                    {
                        throw new Exception("游戏依赖下载失败：" + jarName + "\n\n" + (lastError == null ? "未知错误" : lastError.Message) + "\n\n请检查网络后重试。");
                    }
                }
            });
        }

        // =============================================================
        // 第 3 步：按需编译最新源码到 out\classes
        // =============================================================

        private static bool NeedsRebuild()
        {
            if (!File.Exists(_stampFile) || !Directory.Exists(_classesDir)) return true;
            DateTime stampTime = File.GetLastWriteTimeUtc(_stampFile);
            DateTime newest = DateTime.MinValue;

            string srcJavaDir = Path.Combine(_root, "src", "main", "java");
            string resDir = Path.Combine(_root, "src", "main", "resources");
            if (Directory.Exists(srcJavaDir))
            {
                foreach (string file in Directory.GetFiles(srcJavaDir, "*", SearchOption.AllDirectories))
                {
                    DateTime t = File.GetLastWriteTimeUtc(file);
                    if (t > newest) newest = t;
                }
            }
            if (Directory.Exists(resDir))
            {
                foreach (string file in Directory.GetFiles(resDir, "*", SearchOption.AllDirectories))
                {
                    DateTime t = File.GetLastWriteTimeUtc(file);
                    if (t > newest) newest = t;
                }
            }
            return newest > stampTime;
        }

        private static void CompileIfNeeded(string javaBin)
        {
            if (!NeedsRebuild()) return;

            RunWithProgress("星尘地牢 - 正在编译游戏", false, delegate(ProgressForm form)
            {
                try
                {
                    if (Directory.Exists(_classesDir)) Directory.Delete(_classesDir, true);
                }
                catch (Exception ex)
                {
                    throw new Exception("无法清理旧的编译结果（文件可能被占用）：" + ex.Message);
                }
                Directory.CreateDirectory(_classesDir);

                string srcJavaDir = Path.Combine(_root, "src", "main", "java");
                string resDir = Path.Combine(_root, "src", "main", "resources");

                string[] sources = Directory.GetFiles(srcJavaDir, "*.java", SearchOption.AllDirectories);
                if (sources.Length == 0) throw new Exception("没有找到任何 .java 源码文件。");

                form.SetStatus(string.Format("正在编译游戏（{0} 个源码文件，首次约 10 秒）...", sources.Length));
                form.SetMarquee(true);

                StringBuilder argBuilder = new StringBuilder();
                argBuilder.Append("-encoding UTF-8 ");
                argBuilder.Append("-d ").Append(QuoteArg(_classesDir)).Append(' ');
                argBuilder.Append("--module-path ").Append(QuoteArg(_libsDir)).Append(' ');
                argBuilder.Append("--add-modules javafx.controls ");
                for (int i = 0; i < sources.Length; i++)
                {
                    argBuilder.Append(QuoteArg(sources[i])).Append(' ');
                }

                int exitCode;
                string stdout;
                string stderr;
                RunCapture(Path.Combine(javaBin, "javac.exe"), argBuilder.ToString(), 600000, out exitCode, out stdout, out stderr);
                AppendLog("== 编译 " + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " ==\r\n" + stdout + "\r\n" + stderr + "\r\n");

                if (exitCode != 0)
                {
                    throw new Exception("游戏编译失败（javac 退出码 " + exitCode + "）。\n\n" + TailText(stdout + "\n" + stderr, 14) + "\n\n完整日志：" + _logFile);
                }

                form.SetStatus("正在复制游戏资源...");
                CopyDirectory(resDir, _classesDir);

                File.WriteAllText(_stampFile, DateTime.Now.ToString("o"));
            });
        }

        // =============================================================
        // 第 4 步：启动游戏
        // =============================================================

        private static void LaunchGame(string javaBin)
        {
            string launcherExe = Path.Combine(javaBin, "javaw.exe");
            if (!File.Exists(launcherExe)) launcherExe = Path.Combine(javaBin, "java.exe");

            string args = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
                + " --module-path " + QuoteArg(_libsDir)
                + " --add-modules javafx.controls"
                + " -cp " + QuoteArg(_classesDir)
                + " " + MainClassName;

            ProcessStartInfo psi = new ProcessStartInfo(launcherExe, args);
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.WorkingDirectory = _root;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;

            StringBuilder gameOutput = new StringBuilder();
            object gate = new object();

            using (Process process = new Process())
            {
                process.StartInfo = psi;
                DataReceivedEventHandler handler = delegate(object sender, DataReceivedEventArgs e)
                {
                    if (e.Data != null)
                    {
                        lock (gate) { gameOutput.AppendLine(e.Data); }
                    }
                };
                process.OutputDataReceived += handler;
                process.ErrorDataReceived += handler;
                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                // 观察 10 秒：若游戏进程立即异常退出，说明启动失败，弹窗给出日志
                bool exitedEarly = process.WaitForExit(10000);
                string captured;
                lock (gate) { captured = gameOutput.ToString(); }
                AppendLog("== 启动游戏 " + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " ==\r\n" + captured + "\r\n");

                if (exitedEarly && process.ExitCode != 0)
                {
                    ShowError("游戏启动失败",
                        "游戏未能正常启动（退出码 " + process.ExitCode + "）。\n\n" + TailText(captured, 15) + "\n\n完整日志：" + _logFile);
                }
            }
        }

        // =============================================================
        // 通用工具
        // =============================================================

        private static void RunWithProgress(string title, bool cancellable, Action<ProgressForm> work)
        {
            using (ProgressForm form = new ProgressForm(title, cancellable))
            {
                Exception failure = null;
                bool canceled = false;

                form.Load += delegate
                {
                    Task.Factory.StartNew(delegate
                    {
                        try
                        {
                            work(form);
                        }
                        catch (OperationCanceledException) { canceled = true; }
                        catch (Exception ex) { failure = ex; }
                        finally
                        {
                            form.CloseAfterWork();
                        }
                    });
                };

                Application.Run(form);

                if (canceled) throw new OperationCanceledException();
                if (failure != null) throw failure;
            }
        }

        private static void RunCapture(string exePath, string arguments, int timeoutMs, out int exitCode, out string stdout, out string stderr)
        {
            ProcessStartInfo psi = new ProcessStartInfo(exePath, arguments);
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;

            using (Process process = Process.Start(psi))
            {
                Task<string> outTask = process.StandardOutput.ReadToEndAsync();
                Task<string> errTask = process.StandardError.ReadToEndAsync();
                if (!process.WaitForExit(timeoutMs))
                {
                    try { process.Kill(); } catch { }
                    throw new Exception("程序执行超时：" + Path.GetFileName(exePath));
                }
                stdout = outTask.Result;
                stderr = errTask.Result;
                exitCode = process.ExitCode;
            }
        }

        private static void DownloadFile(ProgressForm form, string url, string destPath, string statusPrefix, int percentBase, int percentTotal)
        {
            string partPath = destPath + ".part";
            DeleteFileQuiet(partPath);

            try
            {
                using (WebClient client = new TimeoutWebClient())
                {
                    client.Headers.Add("User-Agent", "StardustLauncher/1.0");

                    Exception failure = null;
                    ManualResetEventSlim finished = new ManualResetEventSlim(false);

                    client.DownloadProgressChanged += delegate(object sender, DownloadProgressChangedEventArgs e)
                    {
                        if (e.TotalBytesToReceive <= 0) return;
                        long receivedMb = e.BytesReceived / 1048576;
                        long totalMb = e.TotalBytesToReceive / 1048576;
                        form.SetStatus(string.Format("{0} {1}MB / {2}MB（{3}%）", statusPrefix, receivedMb, totalMb, e.ProgressPercentage));
                        form.SetPercent((percentBase * 100 + e.ProgressPercentage) / percentTotal);
                    };

                    client.DownloadFileCompleted += delegate(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
                    {
                        if (e.Error != null) failure = e.Error;
                        else if (e.Cancelled) failure = new OperationCanceledException();
                        finished.Set();
                    };

                    form.SetMarquee(false);
                    client.DownloadFileAsync(new Uri(url), partPath);

                    while (!finished.Wait(200))
                    {
                        if (form.CancelRequested)
                        {
                            try { client.CancelAsync(); } catch { }
                        }
                    }

                    if (failure != null) throw failure;
                    if (form.CancelRequested) throw new OperationCanceledException();
                }

                DeleteFileQuiet(destPath);
                File.Move(partPath, destPath);
            }
            catch
            {
                DeleteFileQuiet(partPath);
                throw;
            }
        }

        private static bool IsZipReadable(string path)
        {
            try
            {
                using (ZipFile.OpenRead(path)) { }
                return true;
            }
            catch
            {
                return false;
            }
        }

        private static void CopyDirectory(string fromDir, string toDir)
        {
            if (!Directory.Exists(fromDir)) return;
            Directory.CreateDirectory(toDir);

            string[] files = Directory.GetFiles(fromDir);
            for (int i = 0; i < files.Length; i++)
            {
                File.Copy(files[i], Path.Combine(toDir, Path.GetFileName(files[i])), true);
            }
            string[] subDirs = Directory.GetDirectories(fromDir);
            for (int i = 0; i < subDirs.Length; i++)
            {
                CopyDirectory(subDirs[i], Path.Combine(toDir, Path.GetFileName(subDirs[i])));
            }
        }

        private static string QuoteArg(string value)
        {
            return "\"" + value + "\"";
        }

        private static string TailText(string text, int maxLines)
        {
            if (string.IsNullOrEmpty(text)) return "（无输出）";
            string[] lines = text.Replace("\r\n", "\n").Split('\n');
            int start = Math.Max(0, lines.Length - maxLines);
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < lines.Length; i++)
            {
                if (lines[i].Trim().Length == 0) continue;
                builder.AppendLine(lines[i]);
            }
            string result = builder.ToString().TrimEnd();
            return result.Length == 0 ? "（无输出）" : result;
        }

        private static void AppendLog(string text)
        {
            try
            {
                Directory.CreateDirectory(_outDir);
                File.AppendAllText(_logFile, text, new UTF8Encoding(false));
            }
            catch { }
        }

        private static void ShowError(string title, string message)
        {
            MessageBox.Show(message, "星尘地牢 - " + title, MessageBoxButtons.OK, MessageBoxIcon.Error);
        }

        private static void DeleteFileQuiet(string path)
        {
            try { if (File.Exists(path)) File.Delete(path); }
            catch { }
        }

        private static void DeleteDirectoryQuiet(string path)
        {
            try { if (Directory.Exists(path)) Directory.Delete(path, true); }
            catch { }
        }

        // 带超时的下载客户端：连接/响应超时 20 秒，数据间断超时 45 秒，
        // 用于在某个下载源不可用时快速切换到下一个源。
        private sealed class TimeoutWebClient : WebClient
        {
            protected override WebRequest GetWebRequest(Uri address)
            {
                WebRequest request = base.GetWebRequest(address);
                HttpWebRequest httpRequest = request as HttpWebRequest;
                if (httpRequest != null)
                {
                    httpRequest.Timeout = 20000;
                    httpRequest.ReadWriteTimeout = 45000;
                    httpRequest.AllowAutoRedirect = true;
                }
                return request;
            }
        }
    }

    // =================================================================
    // 进度窗口（下载 / 解压 / 编译时显示，可取消）
    // =================================================================
    internal sealed class ProgressForm : Form
    {
        private readonly bool _cancellable;
        private readonly Label _statusLabel;
        private readonly ProgressBar _progressBar;
        private readonly Button _cancelButton;
        private bool _allowClose;

        public volatile bool CancelRequested;

        public ProgressForm(string title, bool cancellable)
        {
            _cancellable = cancellable;

            Text = title;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            ControlBox = cancellable;
            StartPosition = FormStartPosition.CenterScreen;
            ShowInTaskbar = true;
            ClientSize = new Size(470, 130);

            try { Font = new Font("Microsoft YaHei UI", 9F); }
            catch { }

            _statusLabel = new Label();
            _statusLabel.AutoSize = false;
            _statusLabel.Location = new Point(18, 18);
            _statusLabel.Size = new Size(434, 38);
            _statusLabel.Text = "正在准备...";

            _progressBar = new ProgressBar();
            _progressBar.Location = new Point(18, 62);
            _progressBar.Size = new Size(434, 22);
            _progressBar.Style = ProgressBarStyle.Marquee;

            _cancelButton = new Button();
            _cancelButton.Text = "取消";
            _cancelButton.Location = new Point(364, 92);
            _cancelButton.Size = new Size(88, 26);
            _cancelButton.Visible = cancellable;
            _cancelButton.Click += delegate
            {
                CancelRequested = true;
                _cancelButton.Enabled = false;
                SetStatus("正在取消...");
            };

            Controls.Add(_statusLabel);
            Controls.Add(_progressBar);
            Controls.Add(_cancelButton);

            FormClosing += delegate(object sender, FormClosingEventArgs e)
            {
                if (_allowClose) return;
                e.Cancel = true;
                if (_cancellable)
                {
                    CancelRequested = true;
                    _cancelButton.Enabled = false;
                    SetStatus("正在取消...");
                }
            };
        }

        public void CloseAfterWork()
        {
            _allowClose = true;
            try
            {
                if (IsHandleCreated) BeginInvoke(new MethodInvoker(Close));
            }
            catch { }
        }

        public void SetStatus(string text)
        {
            RunOnUi(delegate { _statusLabel.Text = text; });
        }

        public void SetPercent(int percent)
        {
            if (percent < 0) percent = 0;
            if (percent > 100) percent = 100;
            RunOnUi(delegate
            {
                if (_progressBar.Style != ProgressBarStyle.Blocks) _progressBar.Style = ProgressBarStyle.Blocks;
                _progressBar.Value = percent;
            });
        }

        public void SetMarquee(bool marquee)
        {
            RunOnUi(delegate
            {
                _progressBar.Style = marquee ? ProgressBarStyle.Marquee : ProgressBarStyle.Blocks;
                if (!marquee) _progressBar.Value = 0;
            });
        }

        public void SetCancelEnabled(bool enabled)
        {
            RunOnUi(delegate
            {
                _cancelButton.Enabled = enabled;
            });
        }

        private void RunOnUi(Action action)
        {
            if (IsDisposed) return;
            try
            {
                if (InvokeRequired)
                {
                    if (IsHandleCreated) BeginInvoke(action);
                }
                else
                {
                    action();
                }
            }
            catch { }
        }
    }

    // =================================================================
    // “缺少 Java” 引导窗口：用户自行下载安装 JDK 17，装好后可点“重试”
    // =================================================================
    internal sealed class JavaRequiredForm : Form
    {
        private const string MirrorUrl = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/";
        private const string OfficialUrl = "https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64";

        public bool RetryRequested;

        public JavaRequiredForm()
        {
            Text = "星尘地牢 - 需要先安装 Java 17";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            ShowInTaskbar = true;
            ClientSize = new Size(560, 330);

            try { Font = new Font("Microsoft YaHei UI", 9F); }
            catch { }

            Label message = new Label();
            message.AutoSize = false;
            message.Location = new Point(20, 16);
            message.Size = new Size(520, 200);
            message.Text = "未检测到 Java 17 运行环境。\n\n"
                + "星尘地牢需要先安装一次 Java 17（免费软件，约 190MB），\n"
                + "安装完成后即可离线游玩，无需重复下载。\n\n"
                + "安装步骤：\n"
                + "  1. 点击下方按钮打开下载页面；\n"
                + "  2. 下载 Windows x64 版 JDK 17（请选 JDK，不要选 JRE）；\n"
                + "  3. 双击安装包，一路“下一步”即可；\n"
                + "  4. 安装完成后，点击“我已安装，重试”。";

            Button mirrorButton = new Button();
            mirrorButton.Text = "打开下载页面（国内镜像，推荐）";
            mirrorButton.Location = new Point(20, 228);
            mirrorButton.Size = new Size(250, 32);
            mirrorButton.Click += delegate { OpenUrl(MirrorUrl); };

            Button officialButton = new Button();
            officialButton.Text = "打开官方下载页面";
            officialButton.Location = new Point(285, 228);
            officialButton.Size = new Size(200, 32);
            officialButton.Click += delegate { OpenUrl(OfficialUrl); };

            Button retryButton = new Button();
            retryButton.Text = "我已安装，重试";
            retryButton.Location = new Point(20, 272);
            retryButton.Size = new Size(220, 36);
            retryButton.Click += delegate
            {
                RetryRequested = true;
                Close();
            };
            AcceptButton = retryButton;

            Button exitButton = new Button();
            exitButton.Text = "退出";
            exitButton.Location = new Point(285, 272);
            exitButton.Size = new Size(120, 36);
            exitButton.Click += delegate { Close(); };
            CancelButton = exitButton;

            Controls.Add(message);
            Controls.Add(mirrorButton);
            Controls.Add(officialButton);
            Controls.Add(retryButton);
            Controls.Add(exitButton);
        }

        private static void OpenUrl(string url)
        {
            try
            {
                ProcessStartInfo info = new ProcessStartInfo(url);
                info.UseShellExecute = true;
                Process.Start(info);
            }
            catch { }
        }
    }
}
