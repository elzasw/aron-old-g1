import react from "@vitejs/plugin-react";
import fs from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";
import url from "node:url";
import { defineConfig, loadEnv, PluginOption } from "vite";


const defaultEndpoint = 'http://localhost:8088';
export default ({ mode }) => {
  // Load environment variables
  process.env = { ...process.env, ...loadEnv(mode, process.cwd(), "") }
  const endpoint = process.env.ENDPOINT || defaultEndpoint;

  // https://vitejs.dev/config/
  return defineConfig({
    plugins: [react(), reactVirtualized()],
    server: {
      port: 3000,
      hmr: {
        port: 3001
      },
      proxy: {
        '/api': {
          target: endpoint,
          changeOrigin: true,
        },
        '/logout': {
          target: endpoint,
          changeOrigin: false,
        },
      },
    },
  });
}

const WRONG_CODE = `import { bpfrpt_proptype_WindowScroller } from "../WindowScroller.js";`;

function reactVirtualized(): PluginOption {

  return {
    name: "flat:react-virtualized",
    // Note: we cannot use the `transform` hook here
    //       because libraries are pre-bundled in vite directly,
    //       plugins aren't able to hack that step currently.
    //       so instead we manually edit the file in node_modules.
    //       all we need is to find the timing before pre-bundling.
    configResolved: async () => {
      const require = createRequire(import.meta.url);
      const reactVirtualizedPath = require.resolve("react-virtualized");
      const { pathname: reactVirtualizedFilePath } = new url.URL(
        reactVirtualizedPath,
        import.meta.url
      );
      const file = reactVirtualizedFilePath.replace(
        path.join("dist", "commonjs", "index.js"),
        path.join("dist", "es", "WindowScroller", "utils", "onScroll.js")
      );
      const code = await fs.readFile(file, "utf-8");
      const modified = code.replace(WRONG_CODE, "");
      await fs.writeFile(file, modified);
    },
  };
}
