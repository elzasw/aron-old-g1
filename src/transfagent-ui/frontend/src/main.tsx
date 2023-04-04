import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";
import './index.css'

const serverContextPath = window.serverContextPath;

const router = createBrowserRouter(
    [
        {
            path: "/",
            element: <App />,
        }
    ],
    {
        basename: serverContextPath.startsWith('http') ? '' : serverContextPath,
    },
  );

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
)
