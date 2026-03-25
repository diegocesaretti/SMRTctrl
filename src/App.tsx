import React, { useState, useEffect } from "react";
import { 
  FileCode, 
  Download, 
  Terminal, 
  Home, 
  Cpu, 
  CheckCircle2, 
  Copy,
  ExternalLink,
  ChevronRight,
  Info
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { cn } from "./lib/utils";

const FILES = [
  { name: "manifest.json", path: "/custom_components/bgh_smart/manifest.json", language: "json" },
  { name: "__init__.py", path: "/custom_components/bgh_smart/__init__.py", language: "python" },
  { name: "climate.py", path: "/custom_components/bgh_smart/climate.py", language: "python" },
  { name: "const.py", path: "/custom_components/bgh_smart/const.py", language: "python" },
  { name: "config_flow.py", path: "/custom_components/bgh_smart/config_flow.py", language: "python" },
];

export default function App() {
  const [selectedFile, setSelectedFile] = useState(FILES[2]); // Default to climate.py
  const [fileContent, setFileContent] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    // In a real app we'd fetch from the server
    // For this preview, I'll simulate the content based on what I just wrote
    // (In a real turn, I'd use an API to read the files)
    const fetchFile = async () => {
      try {
        const res = await fetch(`/api/files?path=${encodeURIComponent(selectedFile.path)}`);
        const data = await res.json();
        setFileContent(data.content);
      } catch (err) {
        setFileContent("# Error loading file content. Please check the file explorer.");
      }
    };
    fetchFile();
  }, [selectedFile]);

  const handleCopy = () => {
    navigator.clipboard.writeText(fileContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen bg-[#F5F5F7] font-sans text-[#1D1D1F]">
      {/* Sidebar */}
      <div className="fixed left-0 top-0 bottom-0 w-72 bg-white border-r border-[#D2D2D7] p-6 flex flex-col">
        <div className="flex items-center gap-3 mb-10">
          <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
            <Home className="w-6 h-6" />
          </div>
          <div>
            <h1 className="font-bold text-lg leading-tight">BGH Smart</h1>
            <p className="text-[10px] uppercase tracking-widest text-[#86868B] font-semibold">HA Integration</p>
          </div>
        </div>

        <nav className="space-y-1 flex-1">
          <p className="text-[11px] font-bold text-[#86868B] uppercase tracking-wider mb-4 px-3">Integration Files</p>
          {FILES.map((file) => (
            <button
              key={file.path}
              onClick={() => setSelectedFile(file)}
              className={cn(
                "w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group",
                selectedFile.path === file.path 
                  ? "bg-blue-50 text-blue-600" 
                  : "text-[#424245] hover:bg-[#F5F5F7]"
              )}
            >
              <FileCode className={cn("w-4 h-4", selectedFile.path === file.path ? "text-blue-600" : "text-[#86868B]")} />
              {file.name}
              {selectedFile.path === file.path && (
                <motion.div layoutId="active" className="ml-auto">
                  <ChevronRight className="w-4 h-4" />
                </motion.div>
              )}
            </button>
          ))}
        </nav>

        <div className="mt-auto pt-6 border-t border-[#D2D2D7]">
          <div className="bg-[#F5F5F7] rounded-xl p-4">
            <div className="flex items-center gap-2 mb-2">
              <Info className="w-4 h-4 text-blue-600" />
              <span className="text-xs font-bold">Installation</span>
            </div>
            <p className="text-[11px] text-[#86868B] leading-relaxed">
              Copy the <code className="bg-white px-1 rounded">bgh_smart</code> folder to your Home Assistant's <code className="bg-white px-1 rounded">custom_components</code> directory.
            </p>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="ml-72 p-8 lg:p-12">
        <header className="flex items-center justify-between mb-8">
          <div>
            <div className="flex items-center gap-2 text-sm text-[#86868B] mb-1">
              <span>custom_components</span>
              <ChevronRight className="w-3 h-3" />
              <span>bgh_smart</span>
              <ChevronRight className="w-3 h-3" />
              <span className="text-blue-600 font-medium">{selectedFile.name}</span>
            </div>
            <h2 className="text-3xl font-bold tracking-tight">Source Code</h2>
          </div>
          
          <div className="flex items-center gap-3">
            <button 
              onClick={handleCopy}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-[#D2D2D7] rounded-full text-sm font-semibold hover:bg-[#F5F5F7] transition-all"
            >
              {copied ? <CheckCircle2 className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4" />}
              {copied ? "Copied!" : "Copy Code"}
            </button>
            <button className="flex items-center gap-2 px-4 py-2 bg-[#1D1D1F] text-white rounded-full text-sm font-semibold hover:bg-[#424245] transition-all shadow-lg shadow-black/10">
              <Download className="w-4 h-4" />
              Download ZIP
            </button>
          </div>
        </header>

        {/* Code Editor View */}
        <div className="bg-[#1D1D1F] rounded-2xl shadow-2xl overflow-hidden border border-white/10">
          <div className="flex items-center gap-2 px-4 py-3 bg-[#2D2D2F] border-b border-white/5">
            <div className="flex gap-1.5">
              <div className="w-3 h-3 rounded-full bg-[#FF5F56]" />
              <div className="w-3 h-3 rounded-full bg-[#FFBD2E]" />
              <div className="w-3 h-3 rounded-full bg-[#27C93F]" />
            </div>
            <span className="text-[11px] font-mono text-[#86868B] ml-4 uppercase tracking-widest">{selectedFile.language}</span>
          </div>
          <div className="p-6 overflow-auto max-h-[70vh]">
            <pre className="font-mono text-sm leading-relaxed text-[#E1E1E6]">
              {fileContent || "Loading content..."}
            </pre>
          </div>
        </div>

        {/* Protocol Details */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12">
          <div className="bg-white p-6 rounded-2xl border border-[#D2D2D7]">
            <div className="w-10 h-10 rounded-xl bg-orange-100 flex items-center justify-center text-orange-600 mb-4">
              <Terminal className="w-5 h-5" />
            </div>
            <h3 className="font-bold mb-2">UDP Protocol</h3>
            <p className="text-sm text-[#86868B]">Uses ports 20910 (send) and 20911 (receive) for local network communication.</p>
          </div>
          <div className="bg-white p-6 rounded-2xl border border-[#D2D2D7]">
            <div className="w-10 h-10 rounded-xl bg-green-100 flex items-center justify-center text-green-600 mb-4">
              <Cpu className="w-5 h-5" />
            </div>
            <h3 className="font-bold mb-2">Local Polling</h3>
            <p className="text-sm text-[#86868B]">The integration polls the device status every 30 seconds for real-time updates.</p>
          </div>
          <div className="bg-white p-6 rounded-2xl border border-[#D2D2D7]">
            <div className="w-10 h-10 rounded-xl bg-purple-100 flex items-center justify-center text-purple-600 mb-4">
              <ExternalLink className="w-5 h-5" />
            </div>
            <h3 className="font-bold mb-2">Config Flow</h3>
            <p className="text-sm text-[#86868B]">Supports automatic discovery of the Device ID simply by providing the IP address.</p>
          </div>
        </div>
      </main>
    </div>
  );
}
