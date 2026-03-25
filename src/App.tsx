import React from "react";
import { 
  Home, 
  Terminal, 
  Cpu, 
  ExternalLink,
  ChevronRight,
  Info,
  Github,
  CheckCircle2
} from "lucide-react";
import { motion } from "motion/react";

export default function App() {
  return (
    <div className="min-h-screen bg-[#F5F5F7] font-sans text-[#1D1D1F] p-8 lg:p-12">
      <div className="max-w-4xl mx-auto">
        <header className="flex items-center justify-between mb-12">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-xl shadow-blue-500/20">
              <Home className="w-7 h-7" />
            </div>
            <div>
              <h1 className="text-3xl font-bold tracking-tight">BGH Smart AC</h1>
              <p className="text-sm text-[#86868B] font-medium">Home Assistant Custom Integration</p>
            </div>
          </div>
          <a 
            href="https://github.com/firtman/bgh_smart" 
            target="_blank" 
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-5 py-2.5 bg-[#1D1D1F] text-white rounded-full text-sm font-semibold hover:bg-[#424245] transition-all shadow-lg shadow-black/10"
          >
            <Github className="w-4 h-4" />
            View on GitHub
          </a>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
          <div className="lg:col-span-2 space-y-8">
            <section className="bg-white rounded-3xl p-8 border border-[#D2D2D7] shadow-sm">
              <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
                <Info className="w-5 h-5 text-blue-600" />
                About this Integration
              </h2>
              <p className="text-[#424245] leading-relaxed mb-6">
                This repository contains a pure Home Assistant custom component for BGH Smart AC units. 
                It implements local UDP communication, bypassing the need for cloud services and ensuring 
                fast, reliable control of your air conditioning units.
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  "Local UDP Control",
                  "Manual Configuration",
                  "HVAC Mode Support",
                  "Fan Speed Control",
                  "Temperature Polling",
                  "HACS Compatible"
                ].map((feature) => (
                  <div key={feature} className="flex items-center gap-2 text-sm text-[#424245]">
                    <CheckCircle2 className="w-4 h-4 text-green-500" />
                    {feature}
                  </div>
                ))}
              </div>
            </section>

            <section className="bg-white rounded-3xl p-8 border border-[#D2D2D7] shadow-sm">
              <h2 className="text-xl font-bold mb-4">Installation</h2>
              <div className="space-y-6">
                <div>
                  <h3 className="font-bold text-sm uppercase tracking-wider text-[#86868B] mb-3">Via HACS (Recommended)</h3>
                  <ol className="space-y-3 text-sm text-[#424245] list-decimal list-inside">
                    <li>Open HACS in your Home Assistant instance.</li>
                    <li>Go to <b>Integrations</b>.</li>
                    <li>Add this repository as a <b>Custom Repository</b>.</li>
                    <li>Search for "BGH Smart AC" and install.</li>
                    <li>Restart Home Assistant.</li>
                    <li>Go to Settings &gt; Devices &amp; Services and add the integration.</li>
                    <li>Enter your AC unit's <b>IP Address</b> and <b>MAC Address</b>.</li>
                  </ol>
                </div>
                <div className="pt-6 border-t border-[#D2D2D7]">
                  <h3 className="font-bold text-sm uppercase tracking-wider text-[#86868B] mb-3">Manual Installation</h3>
                  <p className="text-sm text-[#424245] mb-4">
                    Copy the <code className="bg-[#F5F5F7] px-1.5 py-0.5 rounded font-mono text-xs">custom_components/bgh_smart</code> folder to your Home Assistant's <code className="bg-[#F5F5F7] px-1.5 py-0.5 rounded font-mono text-xs">config/custom_components/</code> directory.
                  </p>
                </div>
              </div>
            </section>
          </div>

          <aside className="space-y-6">
            <div className="bg-[#1D1D1F] rounded-3xl p-6 text-white shadow-xl">
              <h3 className="font-bold mb-4 flex items-center gap-2">
                <Terminal className="w-4 h-4 text-blue-400" />
                Protocol Specs
              </h3>
              <div className="space-y-4 text-xs font-mono">
                <div>
                  <p className="text-[#86868B] mb-1">UDP Ports</p>
                  <p>Send: 20910</p>
                  <p>Recv: 20911</p>
                </div>
                <div>
                  <p className="text-[#86868B] mb-1">Status Command</p>
                  <p className="break-all opacity-70">00000000000000[MAC_ADDRESS]590001e4</p>
                </div>
              </div>
            </div>

            <div className="bg-blue-50 rounded-3xl p-6 border border-blue-100">
              <h3 className="font-bold text-blue-900 mb-2 flex items-center gap-2">
                <Cpu className="w-4 h-4" />
                Compatibility
              </h3>
              <p className="text-sm text-blue-800 leading-relaxed">
                Tested with BGH Smart AC units using the Broadlink-based UDP protocol. 
                Works with Home Assistant 2023.x and newer.
              </p>
            </div>
          </aside>
        </div>

        <footer className="text-center pt-8 border-t border-[#D2D2D7]">
          <p className="text-xs text-[#86868B]">
            This project is an independent integration and is not affiliated with BGH.
          </p>
        </footer>
      </div>
    </div>
  );
}
