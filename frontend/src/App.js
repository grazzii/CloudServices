// src/App.js
import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Login from "./components/Login";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/home" element={<h1>Bem-vindo à página inicial!</h1>} />
            </Routes>
        </Router>
    );
}

export default App;
